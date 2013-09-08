package io.dge.slender;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class SlenderClient {

    private Socket socket;
    private InputStream in;
    private PrintWriter out;

    private SessionInfo session;
    private String uuid;

    public SlenderClient(String uuid) throws IOException {
        this.uuid = uuid;
        socket = new Socket("slender.danielge.org", 50007);

        if (socket.isConnected()) {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = socket.getInputStream();
        } else {
            throw new IOException("Cannot connect to server");
        }
    }

    public SessionInfo fetchSessionInfo() throws IOException {
        int numBytes = 12;
        byte[] response = new byte[numBytes];
        int numRead = in.read(response);

        session = new SessionInfo(response);
        return session;
    }

    public SessionInfo getSessionInfo() {
        return session;
    }

    public boolean sendUserInfo(double latitude, double longitude, boolean isGameToPlay) {
        String message = uuid + latitude + "," + longitude + (isGameToPlay ? 1 : 0);
        out.println(message);
        return !out.checkError();
    }

    public void close() {
        out.close();
        try {
            in.close();
            socket.close();
        } catch (IOException e) {
            Log.e("SlenderClient", "Error closing client", e);
        }
    }

    /*
     * Usage example:
     *
     * SlenderClient sc = new SlenderClient();
     * sc.sendUserInfo("6104287060", 10.0, 20.0, true);
     * SessionInfo si = sc.getSessionInfo();
     * if (si != null) {
     *     System.out.print(si.newSessionCountdown);
     * }
     */

    public class SessionInfo {
        public boolean isSessionActive;
        public boolean isClientSlender;
        public int sessionIntensity;
        public int proximityToSlender;
        public int numPartyMembersAround;
        public int numTotalPlayers;
        public int numActivePlayers;
        public int directionArrow;
        public boolean isClientActive;
        public boolean isSlenderVictory;
        public boolean isSessionReqSatisfied;
        public int newSessionCountdown;

        public SessionInfo(byte[] serverInfo) {
            this.isSessionActive = toBool(serverInfo[0]);
            this.isClientSlender = toBool(serverInfo[1]);
            this.sessionIntensity = (int) serverInfo[2];
            this.proximityToSlender = (int) serverInfo[3];
            this.numPartyMembersAround = (int) serverInfo[4];
            this.numTotalPlayers = (int) serverInfo[5];
            this.numActivePlayers = (int) serverInfo[6];
            this.directionArrow = (int) serverInfo[7];
            this.isClientActive = toBool(serverInfo[8]);
            this.isSlenderVictory = toBool(serverInfo[9]);
            this.isSessionReqSatisfied = toBool(serverInfo[10]);
            this.newSessionCountdown = (int) serverInfo[11];
        }


    }
    private static boolean toBool(byte b) {
        return b != 0;
    }
}
