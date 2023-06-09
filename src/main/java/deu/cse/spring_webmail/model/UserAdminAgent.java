/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package deu.cse.spring_webmail.model;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author jongmin
 */
@Slf4j
public class UserAdminAgent {

    private String mysqlServerIp;
    private String mysqlServerPort;
    private String userName;
    private String pass;
    private String jdbcDriver;

    private String server;
    private int port;
    Socket socket = null;
    InputStream is = null;
    OutputStream os = null;
    boolean isConnected = false;
    private String ROOT_ID;
    private String ROOT_PASSWORD;
    private String ADMIN_ID;
    // private final String EOL = "\n";
    private final String EOL = "\r\n";
    private String cwd;

    public UserAdminAgent() {
    }

    public UserAdminAgent(String server, int port, String cwd,
            String root_id, String root_pass, String admin_id,
            String mysqlServerIp, String mysqlServerPort, String userName, String pass, String jdbcDriver) {
        log.debug("UserAdminAgent created: server = " + server + ", port = " + port);
        this.server = server;  // 127.0.0.1
        this.port = port;  // 4555
        this.cwd = cwd;
        this.ROOT_ID = root_id;
        this.ROOT_PASSWORD = root_pass;
        this.ADMIN_ID = admin_id;
        this.mysqlServerIp = mysqlServerIp;
        this.mysqlServerPort = mysqlServerPort;
        this.userName = userName;
        this.pass = pass;
        this.jdbcDriver = jdbcDriver;
        log.debug("isConnected = {}, root.id = {}", isConnected, ROOT_ID);
        log.debug("UserAdminAgent(): mysqlServerIp = {}, jdbvDriver = {}", mysqlServerIp, jdbcDriver);

        try {
            socket = new Socket(server, port);
            is = socket.getInputStream();
            os = socket.getOutputStream();
        } catch (Exception e) {
            log.error("UserAdminAgent 생성자 예외: {}", e.getMessage());
        }

        isConnected = connect();
    }

    /**
     *
     * @param userId
     * @param password
     * @return a boolean value as follows: - true: addUser operation successful
     * - false: addUser operation failed
     */
    // return value:
    //   - true: addUser operation successful
    //   - false: addUser operation failed
    public boolean addUser(String userId, String password) {
        boolean status = false;
        byte[] messageBuffer = new byte[1024];

        log.debug("addUser() called");
        if (!isConnected) {
            return status;
        }

        try {
            // 1: "adduser" command
            String addUserCommand = "adduser " + userId + " " + password + EOL;
            os.write(addUserCommand.getBytes());

            // 2: response for "adduser" command
            java.util.Arrays.fill(messageBuffer, (byte) 0);
            //if (is.available() > 0) {
            is.read(messageBuffer);
            String recvMessage = new String(messageBuffer);
            log.debug(recvMessage);
            //}
            // 3: 기존 메일사용자 여부 확인
            if (recvMessage.contains("added")) {

                status = true;
            } else {
                status = false;
            }
            // 4: 연결 종료
            quit();
            System.out.flush();  // for test
            socket.close();
        } catch (Exception ex) {
            log.error("addUser 예외: {}", ex.getMessage());
            status = false;
        } finally {
            // 5: 상태 반환
            return status;
        }
    }  // addUser()

    public boolean addUserDB(String username, String userId, String password) {
        final String JDBC_URL = String.format("jdbc:mysql://%s:%s/mail?serverTimezone=Asia/Seoul", mysqlServerIp, mysqlServerPort);

        log.debug("JDBC_URL = {}", JDBC_URL);

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Class.forName(jdbcDriver);

            conn = DriverManager.getConnection(JDBC_URL, this.userName, this.pass);
            String sql = "INSERT INTO userinfo VALUES(?,?,?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, username);
            pstmt.setString(2, userId);
            pstmt.setString(3, password);

            pstmt.executeUpdate();

            pstmt.close();
            conn.close();

            return true;

        } catch (Exception ex) {
            log.error("오류가 발생했습니다. (발생오류: {})", ex.getMessage());
            return false;
        }
    }

    public List<String> getUserList() {
        List<String> userList = new LinkedList<String>();
        byte[] messageBuffer = new byte[1024];

        log.info("root.id = {}, root.password = {}", ROOT_ID, ROOT_PASSWORD);

        if (!isConnected) {
            return userList;
        }

        try {
            // 1: "listusers" 명령 송신
            String command = "listusers " + EOL;
            os.write(command.getBytes());

            // 2: "listusers" 명령에 대한 응답 수신
            java.util.Arrays.fill(messageBuffer, (byte) 0);
            is.read(messageBuffer);

            // 3: 응답 메시지 처리
            String recvMessage = new String(messageBuffer);
            log.debug("recvMessage = {}", recvMessage);
            userList = parseUserList(recvMessage);

            quit();
        } catch (Exception ex) {
            log.error("getUserList(): 예외 = {}", ex.getMessage());
        } finally {
            return userList;
        }
    }  // getUserList()

    private List<String> parseUserList(String message) {
        List<String> userList = new LinkedList<String>();

        // UNIX 형식을 윈도우 형식으로 변환하여 처리
        message = message.replace("\r\n", "\n");

        // 1: 줄 단위로 나누기
        String[] lines = message.split("\n");
        // 2: 첫 번째 줄에는 등록된 사용자 수에 대한 정보가 있음.
        //    예) Existing accounts 7
        String[] firstLine = lines[0].split(" ");
        int numberOfUsers = Integer.parseInt(firstLine[2]);

        // 3: 두 번째 줄부터는 각 사용자 ID 정보를 보여줌.
        //    예) user: admin
        for (int i = 1; i <= numberOfUsers; i++) {
            // 3.1: 한 줄을 구분자 " "로 나눔.
            String[] userLine = lines[i].split(" ");
            // 3.2 사용자 ID가 관리자 ID와 일치하는 지 여부 확인
            if (!userLine[1].equals(ADMIN_ID)) {
                userList.add(userLine[1]);
            }
        }
        return userList;
    } // parseUserList()

    public boolean deleteUsers(String[] userList) {
        boolean status = false;

        final String JDBC_URL = String.format("jdbc:mysql://%s:%s/mail?serverTimezone=Asia/Seoul", mysqlServerIp, mysqlServerPort);

        log.debug("JDBC_URL = {}", JDBC_URL);

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            for (int i = 0; i < userList.length; i++) {
                Class.forName(jdbcDriver);
                conn = DriverManager.getConnection(JDBC_URL, this.userName, this.pass);
                String sql = "DELETE FROM userinfo WHERE userid = ? ";
                PreparedStatement pstmt = conn.prepareStatement(sql);

                pstmt.setString(1, userList[i]);

                pstmt.executeUpdate();

                status = true;

                if (pstmt != null) {
                    pstmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            }

        } catch (Exception ex) {
            log.error("오류가 발생했습니다. (발생오류: {})", ex.getMessage());
            return status;
        }
        return status;

    }  // deleteUsers()

    public boolean verify(String userid) {
        boolean status = false;
        byte[] messageBuffer = new byte[1024];

        try {
            // --> verify userid
            String verifyCommand = "verify " + userid;
            os.write(verifyCommand.getBytes());

            // read the result for verify command
            // <-- User userid exists   or
            // <-- User userid does not exist
            is.read(messageBuffer);
            String recvMessage = new String(messageBuffer);
            if (recvMessage.contains("exists")) {
                status = true;
            }

            quit();  // quit command
        } catch (IOException ex) {
            log.error("verify(): 예외 = {}", ex.getMessage());
        } finally {
            return status;
        }
    }

    private boolean connect() {
        byte[] messageBuffer = new byte[1024];
        boolean returnVal = false;
        String sendMessage;
        String recvMessage;

        log.info("connect() : root.id = {}, root.password = {}", ROOT_ID, ROOT_PASSWORD);

        // root 인증: id, passwd - default: root
        // 1: Login Id message 수신
        try {
            is.read(messageBuffer);
            recvMessage = new String(messageBuffer);

            // 2: rootId 송신
            sendMessage = ROOT_ID + EOL;
            os.write(sendMessage.getBytes());

            // 3: Password message 수신
            java.util.Arrays.fill(messageBuffer, (byte) 0);
            is.read(messageBuffer);
            recvMessage = new String(messageBuffer);

            // 4: rootPassword 송신
            sendMessage = ROOT_PASSWORD + EOL;
            os.write(sendMessage.getBytes());

            // 5: welcome message 수신
            java.util.Arrays.fill(messageBuffer, (byte) 0);
            // if (is.available() > 0) {
            is.read(messageBuffer);
            recvMessage = new String(messageBuffer);

            if (recvMessage.contains("Welcome")) {
                returnVal = true;
            } else {
                returnVal = false;
            }
        } catch (Exception e) {
            log.error("connect() 예외: {}", e.getMessage());
        }

        return returnVal;
    }  // connect()

    public boolean quit() {
        byte[] messageBuffer = new byte[1024];
        boolean status = false;
        // quit
        try {
            // 1: quit 명령 송신
            String quitCommand = "quit" + EOL;
            os.write(quitCommand.getBytes());
            // 2: quit 명령에 대한 응답 수신
            java.util.Arrays.fill(messageBuffer, (byte) 0);
            //if (is.available() > 0) {
            is.read(messageBuffer);
            // 3: 메시지 분석
            String recvMessage = new String(messageBuffer);
            if (recvMessage.contains("closed")) {
                status = true;
            } else {
                status = false;
            }
        } catch (IOException ex) {
            log.error("quit() 예외: {}", ex);
        } finally {
            return status;
        }
    }

}
