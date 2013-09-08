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

    public String getUuid() {
        return uuid;
    }

    public SlenderClient(String uuid) throws IOException {
        this.uuid = uuid;
        connect();
    }

    private void connect() throws IOException {
        socket = new Socket("slender.danielge.org", 50007);

        if (socket.isConnected()) {
            out = new PrintWriter(socket.getOutputStream());
            in = socket.getInputStream();
        } else {
            throw new IOException("Cannot connect to server");
        }

    }

    public SessionInfo fetchSessionInfo() {
        int numBytes = 12;
        byte[] response = new byte[numBytes];
        try {
            int numRead = in.read(response);
        } catch (IOException e) {
            return null;
        }

        session = new SessionInfo(response);
        return session;
    }

    public SessionInfo getSessionInfo() {
        return session;
    }

    public void reset() throws IOException {
        close();
        connect();
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
        public Direction directionArrow;
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
            this.directionArrow = toDirection((int) serverInfo[7]);
            this.isClientActive = toBool(serverInfo[8]);
            this.isSlenderVictory = toBool(serverInfo[9]);
            this.isSessionReqSatisfied = toBool(serverInfo[10]);
            this.newSessionCountdown = (int) serverInfo[11];
        }

        private Direction toDirection (int num) {
            switch (num) {
                case 0: return Direction.NORTH;
                case 1: return Direction.NORTHEAST;
                case 2: return Direction.EAST;
                case 3: return Direction.SOUTHEAST;
                case 4: return Direction.SOUTH;
                case 5: return Direction.SOUTHWEST;
                case 6: return Direction.WEST;
                case 7: return Direction.NORTHWEST;
                default: return Direction.INVALID;
            }
        }

    }
    private static boolean toBool(byte b) {
        return b != 0;
    }
}
