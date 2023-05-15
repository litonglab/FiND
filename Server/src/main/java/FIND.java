import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FIND {
    static SplayTree splayTree; // Splay加速模糊匹配（待改进）
    static {
        try {
            splayTree = new SplayTree();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        int PORT = 5000, len;
        ServerSocket server = new ServerSocket(PORT);
        while(true) { // 循环接受客户端请求
            Socket socket = server.accept();
            List<String> reader = new ArrayList<>();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                reader.add(line);
            }

            if (reader.size() == 0) {
                continue;
            }
            String option = reader.get(0);
            if (option.equals(new String("update"))) { // 更新请求
                String beacon = reader.get(1);
                List<Wifi> wifiList = new ArrayList<>();
                for (int i = 2; i < reader.size(); i ++ ) {
                    wifiList.add(new Wifi(reader.get(i)));
                }
                // 数据库更新
                if(splayTree.beaconToIndex.containsKey(beacon)){
                    splayTree.update(beacon, wifiList);
                } else {
                    splayTree.insert(beacon,wifiList);
                }
                System.out.println("update: wifiList Size: " + wifiList.size());
                System.out.println("update: beaconID: " + beacon);
            } else { // 匹配请求
                List<Wifi> wifiList = new ArrayList<>();
                for (int i = 1; i < reader.size(); i++) {
                    wifiList.add(new Wifi(reader.get(i)));
                }
                String result = splayTree.getBeaconId(wifiList); // 数据库查询

                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(result.getBytes(StandardCharsets.UTF_8));
                outputStream.close(); // 返回信息给客户端
                System.out.println("startScan: wifiList Size: " + wifiList.size());
                System.out.println("startScan: beaconID: " + result);
            }
        }
    }
}
