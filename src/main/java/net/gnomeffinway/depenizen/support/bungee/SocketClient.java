package net.gnomeffinway.depenizen.support.bungee;

import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.scripts.queues.core.InstantQueue;
import net.gnomeffinway.depenizen.Depenizen;
import net.gnomeffinway.depenizen.events.bungee.ProxyPingScriptEvent;
import net.gnomeffinway.depenizen.objects.bungee.dServer;
import net.gnomeffinway.depenizen.support.bungee.packets.*;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class SocketClient implements Runnable {

    private String ipAddress;
    private int port;
    private String password;
    private String registrationName;
    private int timeout;
    private Socket socket;
    private BukkitTask task;
    private boolean isConnected;
    private DataOutputStream output;
    private DataInputStream input;

    public SocketClient(String ipAddress, int port, String password, String name, int timeout) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.password = md5(password);
        this.registrationName = name;
        this.timeout = timeout;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void send(Packet packet) {
        try {
            DataSerializer data = new DataSerializer();

            packet.serialize(data);

            byte[] encryptedData = encryptOrDecrypt(this.password, data.toByteArray());
            this.output.writeInt(encryptedData.length);
            this.output.write(encryptedData);
            this.output.flush();
        } catch(Exception e) {
            dB.echoError(e);
            this.close("Error sending data to server: " + e.getMessage());
        }
    }

    public void connect() {
        if (!isConnected) {
            try {
                this.socket = new Socket();
                this.socket.connect(new InetSocketAddress(this.ipAddress, this.port), timeout);
                this.socket.setSoTimeout(timeout);
                this.output = new DataOutputStream(this.socket.getOutputStream());
                this.input = new DataInputStream(this.socket.getInputStream());
                this.isConnected = true;
                this.task = Bukkit.getServer().getScheduler().runTaskAsynchronously(Depenizen.getCurrentInstance(), this);
                this.send(new ClientPacketOutRegister(this.registrationName));
            } catch (IOException e) {
                dB.log("Error while connecting to BungeeCord Socket: " + e.getMessage());
            } catch (Exception e) {
                dB.echoError(e);
            }
        }
    }

    public void close() {
        close(null);
    }

    public void close(String reason) {
        if (isConnected) {
            if (reason != null) {
                dB.log("Disconnected from BungeeCord Socket: " + reason);
            }
            this.isConnected = false;
            this.task.cancel();
            this.task = null;
            try {
                if (this.output != null) this.output.close();
                if (this.input != null) this.input.close();
                if (this.socket != null) this.socket.close();
            } catch (Exception e) {
                dB.echoError(e);
            }
        }
    }

    @Override
    public void run() {
        if (this.socket != null && this.socket.isConnected()) {
            try {
                byte[] buffer;
                while (this.isConnected) {
                    int receivedEncryptedLength = this.input.readInt();
                    if (receivedEncryptedLength == -1) {
                        this.close("Connection failed");
                        break;
                    }

                    buffer = new byte[receivedEncryptedLength];

                    this.input.read(buffer);
                    byte[] encryptedBytes = new byte[receivedEncryptedLength];
                    System.arraycopy(buffer, 0, encryptedBytes, 0, encryptedBytes.length);
                    byte[] decryptedBytes = encryptOrDecrypt(this.password, encryptedBytes);

                    DataDeserializer data = new DataDeserializer(decryptedBytes);

                    int packetType = data.readInt();

                    if (packetType == 0x00) {
                        ClientPacketInAcceptRegister packet = new ClientPacketInAcceptRegister();
                        packet.deserialize(data);
                        if (packet.isAccepted()) {
                            dB.log("Successfully registered name with the server");
                            for (String server : packet.getServerList()) {
                                if (!server.isEmpty())
                                    dServer.addOnlineServer(server);
                            }
                        }
                        else
                            this.close("Specified name in config.yml is already registered to the server");
                    }
                    else if (packetType == 0x01) {
                        ClientPacketInServer packet = new ClientPacketInServer();
                        packet.deserialize(data);
                        if (packet.getAction() == ClientPacketInServer.Action.REGISTERED)
                            dServer.addOnlineServer(packet.getServerName());
                        else if (packet.getAction() == ClientPacketInServer.Action.DISCONNECTED)
                            dServer.removeOnlineServer(packet.getServerName());
                    }
                    else if (packetType == 0x02) {
                        ClientPacketInScript packet = new ClientPacketInScript();
                        packet.deserialize(data);
                        InstantQueue queue = new InstantQueue(ScriptQueue.getNextId("BUNGEE_CMD"));
                        queue.addEntries(packet.getScriptEntries());
                        queue.getAllDefinitions().putAll(packet.getDefinitions());
                        queue.start();
                    }
                    else if (packetType == 0x03) {
                        ClientPacketInEvent packet = new ClientPacketInEvent();
                        packet.deserialize(data);
                        if (packet.getEventName().equals("ProxyPing")) {
                            ProxyPingScriptEvent event = ProxyPingScriptEvent.instance;
                            if (event != null) {
                                Map<String, String> context = packet.getContext();
                                event.address = new Element(context.get("address"));
                                event.numPlayers = new Element(context.get("num_players"));
                                event.motd = new Element(context.get("motd"));
                                event.maxPlayers = new Element(context.get("max_players"));
                                event.version = new Element(context.get("version"));
                                event.fire();
                                if (packet.shouldSendResponse()) {
                                    Map<String, String> determinations = new HashMap<String, String>();
                                    determinations.put("num_players", event.numPlayers.asString());
                                    determinations.put("max_players", event.maxPlayers.asString());
                                    determinations.put("motd", event.motd.asString());
                                    determinations.put("version", event.version.asString());
                                    ClientPacketOutEventResponse response =
                                            new ClientPacketOutEventResponse(packet.getEventId(), determinations);
                                    send(response);
                                }
                            }
                        }
                    }
                    // 0x04 (EventSubscribe) is outbound
                    else {
                        this.close("Received invalid packet from server: " + packetType);
                    }
                }

            } catch (IllegalStateException e) {
                this.close("Password is incorrect");
            } catch (SocketTimeoutException e) {
                this.close("Connection timed out");
            } catch (IOException e) {
                this.close("Server socket closed");
            } catch (Exception e) {
                this.close("Error receiving data from server: " + e.getMessage());
                dB.echoError(e);
            }
        }
    }

    private static String md5(String string) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(string.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes)
                sb.append(Integer.toHexString(b & 0xFF | 0x100)).substring(1, 3);
            return sb.toString();
        }
        catch (Exception e) {
            dB.echoError(e);
            return null;
        }
    }

    private static byte[] encryptOrDecrypt(String password, byte[] data) throws Exception {
        byte[] result = new byte[data.length];
        byte[] passwordBytes = password.getBytes("UTF-8");
        for (int i = 0; i < data.length; i++)
            result[i] = ((byte)(data[i] ^ passwordBytes[(i % passwordBytes.length)]));
        return result;
    }
}