package chat_room;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;

public class Server
{
    public static void main(String[] args) throws Exception
    {
        //建立服务器ServerSocket
        ServerSocket ss = new ServerSocket(6666);
        //提示Server建立成功
        System.out.println("Server online... " + ss.getInetAddress().getLocalHost().getHostAddress() + ", " + 6666);
        //监听端口，建立连接并开启新的ServerThread线程来服务此连接
        while(true)
        {
            //接收客户端Socket
            Socket s = ss.accept();
            //提取客户端IP和端口
            String ip = s.getInetAddress().getHostAddress();
            int port = s.getPort();
            //建立新的服务器线程, 向该线程提供服务器ServerSocket，客户端Socket，客户端IP和端口
            new Thread(new ServerThread(s, ss, ip, port)).start();
        }
    }
}

class ServerThread implements Runnable
{
    //获取的客户端Socket
    Socket s = null;
    //获取的服务器ServerSocket
    ServerSocket ss = null;
    //获取的客户端IP
    String ip = null;
    //获取的客户端端口
    int port = 0;
    //组合客户端的ip和端口字符串得到uid字符串
    String uid = null;

    //静态ArrayList存储所有uid，uid由ip和端口字符串拼接而成
    static ArrayList<String> uid_arr = new ArrayList<String>();
    //静态HashMap存储所有uid, ServerThread对象组成的对
    static HashMap<String, ServerThread> hm = new HashMap<String, ServerThread>();

    public ServerThread(Socket s, ServerSocket ss, String ip, int port)
    {
        this.s = s;
        this.ss = ss;
        this.ip = ip;
        this.port = port;
        uid = ip + ":" + port;
    }

    @Override
    public void run()
    {
        //将当前客户端uid存入ArrayList
        uid_arr.add(uid);
        //将当前uid和ServerThread对存入HashMap
        hm.put(uid, this);

        //时间显示格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        //控制台打印客户端IP和端口
        System.out.println("Client connected: " + uid);

        try
        {
            //获取输入流
            InputStream in = s.getInputStream();
            //获取输出流
            OutputStream out = s.getOutputStream();

            //向当前客户端传输连接成功信息
            String welcome = sdf.format(new Date()) + "\n成功连接服务器...\n服务器IP: "
                    + ss.getInetAddress().getLocalHost().getHostAddress()
                    + ", 端口: 6666\n客户端IP: " + ip + ", 端口: " + port + "\n";
            out.write(welcome.getBytes());

            //广播更新在线名单 
            updateOnlineList(out);

            //准备缓冲区
            byte[] buf = new byte[1024];
            int len = 0;

            //持续监听并转发客户端消息
            while(true)
            {
                len = in.read(buf);
                String msg = new String(buf, 0, len);
                System.out.println(msg);
                //消息类型：退出或者聊天
                String type = msg.substring(0, msg.indexOf("/"));
                //消息本体：空或者聊天内容
                String content = msg.substring(msg.indexOf("/") + 1);
                //根据消息类型分别处理
                //客户端要退出
                if(type.equals("Exit"))
                {
                    //更新ArrayList和HashMap, 删除退出的uid和线程
                    uid_arr.remove(uid_arr.indexOf(uid));
                    hm.remove(uid);
                    //广播更新在线名单
                    updateOnlineList(out);
                    //控制台打印客户端IP和端口
                    System.out.println("Client exited: " + uid);
                    //结束循环，结束该服务线程
                    break;
                }
                //客户端要聊天
                else if(type.equals("Chat"))
                {
                    //提取收信者地址
                    String[] receiver_arr = content.substring(0, content.indexOf("/")).split(",");
                    //提取聊天内容
                    String word = content.substring(content.indexOf("/")+1);
                    //向收信者广播发出聊天信息
                    chatOnlineList(out, uid, receiver_arr, word);
                }
            }
        }
        catch(Exception e){}
    }

    //向所有已连接的客户端更新在线名单
    public void updateOnlineList(OutputStream out) throws Exception
    {
        for(String tmp_uid : uid_arr)
        {
            //获取广播收听者的输出流
            out = hm.get(tmp_uid).s.getOutputStream();
            //将当前在线名单以逗号为分割组合成长字符串一次传送
            StringBuilder sb = new StringBuilder("OnlineListUpdate/");
            for(String member : uid_arr)
            {
                sb.append(member);
                //以逗号分隔uid，除了最后一个
                if(uid_arr.indexOf(member) != uid_arr.size() - 1)
                    sb.append(",");
            }
            out.write(sb.toString().getBytes());
        }
    }

    //向指定的客户端发送聊天消息
    public void chatOnlineList(OutputStream out, String uid, String[] receiver_arr, String word) throws Exception
    {
        for(String tmp_uid : receiver_arr)
        {
            //获取广播收听者的输出流
            out = hm.get(tmp_uid).s.getOutputStream();
            //发送聊天信息
            out.write(("Chat/" + uid + "/" + word).getBytes());
        }
    }
}