import java.io.*;
import java.util.*;
public class SplayTree {
    private static class Node { // 节点类
        private String wifi;
        private int[] son = new int[2];
        private int parent;
        private String beaconId;
        private List<Wifi> wifiList;

        public Node(String beacon, List<Wifi> list, int _parent) {
            beaconId = beacon;
            wifiList = list;
            if (wifiList.size() != 0) {
                wifi = wifiList.get(0).getName();
            }
            parent = _parent;
        }

        public void update(List<Wifi> list) {
            for (Wifi w : list) {
                boolean flag = false;
                for (Wifi p : wifiList) {
                    if (Objects.equals(w.getName(), p.getName())) {
                        p.setRssi(w.getRssi());
                        flag = true;
                    }
                }
                if (!flag) wifiList.add(w);
            }
            Collections.sort(wifiList);
            wifi = wifiList.get(0).getName();
        }

        public void print() {
            System.out.println("beaconId: " + beaconId);
            wifiList.stream().map(value -> "wifiName: " + value.getName() + ' ' + "Rssi: " + value.getRssi()).forEach(System.out::println);
        }
    }

    final int N = 500;
    private Node[] node = new Node[N];
    private int index = 0;
    public int root = 0;
    // 边界哨兵
    final String MAX = "zzzzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzz";
    final String MIN = "!!!!!!!!!!-!!!!-!!!!-!!!!-!!!!!!!!!!";
    // beacon字符串与Node下标的映射
    HashMap<String, Integer> beaconToIndex = new HashMap<>();

    public SplayTree() throws IOException {
        File file = new File("Splay.txt");
        List<Wifi> list = new ArrayList<>();
        node[0] = new Node(".", list, 0);
        Wifi maxWifi = new Wifi(MAX, -50),
                minWifi = new Wifi(MIN, -50);
        list.add(maxWifi);
        insert("*", list);
        list.remove(maxWifi);
        list.add(minWifi);
        insert("-", list);
    }
    // Splay核心函数
    private void rotate(int x) {
        int y = node[x].parent, z = node[y].parent;
        int k = 0, r = 0;
        if (node[y].son[1] == x) k = 1;
        if (node[z].son[1] == y) r = 1;
        node[z].son[r] = x;
        node[x].parent = z;
        node[y].son[k] = node[x].son[k ^ 1];
        node[node[x].son[k ^ 1]].parent = y;
        node[x].son[k ^ 1] = y;
        node[y].parent = x;
    }

    private void splay(int x, int k) {
        while (node[x].parent != k) {
            int y = node[x].parent, z = node[y].parent;
            if (z != k) {
                if ((node[y].son[1] == x) ^ (node[z].son[1] == y)) rotate(x);
                else rotate(y);
            }
            rotate(x);
        }
        if (k == 0) root = x;
    }
    // Splay插入
    public void insert(String beacon, List<Wifi> list) {
        int u = root, p = 0;
        Collections.sort(list);

        while (u != 0) {
            p = u;
            int k = 0;
            if (list.get(0).getName().compareTo(node[u].wifi) > 0) k = 1;
            u = node[u].son[k];
        }

        if (beaconToIndex.containsKey(beacon)) {
            u = beaconToIndex.get(beacon);
        } else {
            u = ++index;
            beaconToIndex.put(beacon, index);
        }

        if (p != 0) {
            int k = 0;
            if (list.get(0).getName().compareTo(node[p].wifi) > 0) k = 1;
            node[p].son[k] = u;
        }
        node[u] = new Node(beacon, list, p);
        splay(u, 0);
    }

    private int getLeft(int u) {
        splay(u, 0);
        int t = node[u].son[0];
        while (node[t].son[1] != 0) t = node[t].son[1];
        return t;
    }

    private int getRight(int u) {
        splay(u, 0);
        int t = node[u].son[1];
        while (node[t].son[0] != 0) t = node[t].son[0];
        return t;
    }

    // Splay已有节点更新
    public void update(String beacon, List<Wifi> list) {
        int u = beaconToIndex.get(beacon);
        int left = getLeft(u);
        int right = getRight(u);
        splay(left, 0);
        splay(right, left);
        node[right].son[0] = 0;
        node[u].update(list);
        insert(beacon, node[u].wifiList);
    }

    public void output(int u) {
        if (node[u].son[0] != 0) output(node[u].son[0]);
        System.out.println("name: " + node[u].wifi + " index: " + u);
        if (node[u].son[1] != 0) output(node[u].son[1]);
    }

    // 获取前驱
    private int getPrecursor(String name, int root) {
        int u = root, res = 2;
        String pre = MIN;
        while (u != 0) {
            String wifi = node[u].wifi;
            if (wifi.compareTo(name) < 0) {
                if (wifi.compareTo(pre) >= 0) {
                    res = u;
                    pre = wifi;
                }
                u = node[u].son[1];
            } else {
                u = node[u].son[0];
            }
        }
        return res;
    }

    // 获取后继
    private int getSucceed(String name, int root) {
        int u = root, res = 1;
        String suc = MAX;
        while (u != 0) {
            String wifi = node[u].wifi;
            if (wifi.compareTo(name) > 0) {
                if (wifi.compareTo(suc) <= 0) {
                    res = u;
                    suc = wifi;
                }
                u = node[u].son[0];
            } else {
                u = node[u].son[1];
            }
        }
        return res;
    }

    // 遍历整棵Splay
    private void getNodeList(int u, List<Node> list) {
        if (node[u].son[0] != 0) getNodeList(node[u].son[0], list);
        list.add(node[u]);
        if (node[u].son[1] != 0) getNodeList(node[u].son[1], list);
    }

    // 模糊匹配值计算（待改进）
    private double getMatchingRate(List<Wifi> p, List<Wifi> q) {
        int r = 0;
        for (Wifi a : p) {
            for (Wifi b : q) {
                if (Objects.equals(a.getName(), b.getName()))
                    r++;
            }
        }
        return (double) r / q.size();
    }

    // 查询函数（待改进）
    public String getBeaconId(List<Wifi> list) {
        Collections.sort(list);
        double matchRate = 0;
        String beaconId = "";
        for (int i = 0; i < Math.min(10, list.size()); i ++ ) {
            Wifi wifi = list.get(i);
            List<Node> n = new ArrayList<>();
            int pre = getPrecursor(wifi.getName(), root);
            int suc = getSucceed(wifi.getName(), root);
            splay(pre, 0);
            splay(suc, pre);
            if (node[suc].son[0] != 0) getNodeList(node[suc].son[0], n);
            for (Node node : n) {
                List<Wifi> wifiList = node.wifiList;
                double m = getMatchingRate(wifiList, list);
                if (m > matchRate) {
                    matchRate = m;
                    beaconId = node.beaconId;
                }
            }
        }
        return beaconId;
    }

    public int getBeaconSize() {
        return index - 2;
    }
}




