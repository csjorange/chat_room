package chat_room;

import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.nio.charset.*;
import java.text.*;

public class Client
{
    //建立客户端Socket
    static Socket s = null;
    //消息接收者uid
    static StringBuilder uidReceiver = null;

    public static void main(String[] args)
    {
        //创建客户端窗口对象
        ClientFrame cframe = new ClientFrame();
        //窗口关闭键无效，必须通过退出键退出客户端以便善后
        cframe.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        //获取本机屏幕横向分辨率
        int w = Toolkit.getDefaultToolkit().getScreenSize().width;
        //获取本机屏幕纵向分辨率
        int h = Toolkit.getDefaultToolkit().getScreenSize().height;
        //将窗口置中
        cframe.setLocation((w - cframe.WIDTH)/2, (h - cframe.HEIGHT)/2);
        //设置客户端窗口为可见
        cframe.setVisible(true);

        try
        {
            //连接服务器
            s = new Socket(InetAddress.getLocalHost(), 6666);
            //获取输入流
            InputStream in = s.getInputStream();
            //获取输出流
            OutputStream out = s.getOutputStream();

            //获取服务端欢迎信息
            byte[] buf = new byte[1024];
            int len = in.read(buf);
            //将欢迎信息打印在聊天消息框内
            cframe.jtaChat.append(new String(buf, 0, len));
            cframe.jtaChat.append("\n");

            //持续等待接收服务器信息直至退出
            while(true)
            {
                in = s.getInputStream();
                len = in.read(buf);
                System.out.println(len);
                //处理服务器传来的消息
                String msg = new String(buf, 0, len);
                //消息类型：更新在线名单或者聊天
                String type = msg.substring(0, msg.indexOf("/"));
                //消息本体：更新后的名单或者聊天内容
                String content = msg.substring(msg.indexOf("/") + 1);
                //根据消息类型分别处理
                //更新在线名单
                if(type.equals("OnlineListUpdate"))
                {
                    //提取在线列表的数据模型
                    DefaultTableModel tbm = (DefaultTableModel) cframe.jtbOnline.getModel();
                    //清除在线名单列表
                    tbm.setRowCount(0);
                    //更新在线名单
                    String[] onlinelist = content.split(",");
                    //逐一添加当前在线者
                    for(String member : onlinelist)
                    {
                        String[] tmp = new String[2];
                        //如果是自己则不在名单中显示
                        if(member.equals(InetAddress.getLocalHost().getHostAddress() + ":" + s.getLocalPort()))
                            continue;
                        //tmp[0] = "";
                        tmp[0] = member.substring(0, member.indexOf(":"));
                        tmp[1] = member.substring(member.indexOf(":") + 1);
                        //添加当前在线者之一
                        tbm.addRow(tmp);
                    }
                    //提取在线列表的渲染模型
                    DefaultTableCellRenderer tbr = new DefaultTableCellRenderer();
                    //表格数据居中显示
                    tbr.setHorizontalAlignment(JLabel.CENTER);
                    cframe.jtbOnline.setDefaultRenderer(Object.class, tbr);
                }
                //聊天
                else if(type.equals("Chat"))
                {
                    String sender = content.substring(0, content.indexOf("/"));
                    String word = content.substring(content.indexOf("/") + 1);
                    //在聊天窗打印聊天信息
                    cframe.jtaChat.append(cframe.sdf.format(new Date()) + "\n来自 " + sender + ":\n" + word + "\n\n");
                    //显示最新消息
                    cframe.jtaChat.setCaretPosition(cframe.jtaChat.getDocument().getLength());
                }
            }
        }
        catch(Exception e)
        {
            cframe.jtaChat.append("服务器挂了.....\n");
            e.printStackTrace();
        }
    }
}

//客户端窗口
class ClientFrame extends JFrame
{
    //时间显示格式
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    //窗口宽度
    final int WIDTH = 800;
    //窗口高度
    final int HEIGHT = 580;

    //创建发送按钮
    JButton btnSend = new JButton("发送");
    //创建清除按钮
    JButton btnClear = new JButton("清屏");
    //创建退出按钮
    JButton btnExit = new JButton("退出");

    //创建消息接收者标签
    JLabel lblReceiver = new JLabel("对谁说？");

    //创建文本输入框, 参数分别为行数和列数
    JTextArea jtaSay = new JTextArea();

    //创建聊天消息框
    JTextArea jtaChat = new JTextArea();

    //当前在线列表的列标题
    String[] colTitles = {"IP", "端口"};
    //当前在线列表的数据
    String[][] rowData = null;
    //创建当前在线列表
    JTable jtbOnline = new JTable
            (
                    new DefaultTableModel(rowData, colTitles)
                    {
                        //表格不可编辑，只可显示
                        @Override
                        public boolean isCellEditable(int row, int column)
                        {
                            return false;
                        }
                    }
            );

    //创建聊天消息框的滚动窗
    JScrollPane jspChat = new JScrollPane(jtaChat);

    //创建当前在线列表的滚动窗
    JScrollPane jspOnline = new JScrollPane(jtbOnline);

    //设置默认窗口属性，连接窗口组件
    public ClientFrame()
    {
        //标题
        setTitle("聊天室");
        //大小
        setSize(WIDTH, HEIGHT);
        //不可缩放
        setResizable(false);
        //设置布局:不适用默认布局，完全自定义
        setLayout(null);

        //设置按钮大小和位置
        btnSend.setBounds(20, 510, 60, 25);
        btnClear.setBounds(140, 510, 60, 25);
        btnExit.setBounds(260, 510, 60, 25);

        //设置标签大小和位置
        lblReceiver.setBounds(20, 420, 300, 30);


        //添加按钮
        this.add(btnSend);
        this.add(btnClear);
        this.add(btnExit);

        //添加标签
        this.add(lblReceiver);

        //设置文本输入框大小和位置
        jtaSay.setBounds(20, 460, 500, 40);
        //添加文本输入框
        this.add(jtaSay);

        //聊天消息框自动换行
        jtaChat.setLineWrap(true);
        //聊天框不可编辑，只用来显示
        jtaChat.setEditable(false);


        //设置滚动窗的水平滚动条属性:不出现
        jspChat.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        //设置滚动窗的垂直滚动条属性:需要时自动出现
        jspChat.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        //设置滚动窗大小和位置
        jspChat.setBounds(20, 20, 500, 400);
        //添加聊天窗口的滚动窗
        this.add(jspChat);

        //设置滚动窗的水平滚动条属性:不出现
        jspOnline.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        //设置滚动窗的垂直滚动条属性:需要时自动出现
        jspOnline.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        //设置当前在线列表滚动窗大小和位置
        jspOnline.setBounds(525, 20, 250, 400);
        //添加当前在线列表
        this.add(jspOnline);

        //添加发送按钮的响应事件
        btnSend.addActionListener
                (
                        new ActionListener()
                        {
                            @Override
                            public void actionPerformed(ActionEvent event)
                            {
                                //显示最新消息
                                jtaChat.setCaretPosition(jtaChat.getDocument().getLength());
                                try
                                {
                                    //有收信人才发送
                                    if(Client.uidReceiver.toString().equals("") == false)
                                    {
                                        //在聊天窗打印发送动作信息
                                        jtaChat.append(sdf.format(new Date()) + "\n发往 " + Client.uidReceiver.toString() + ":\n");
                                        //显示发送消息
                                        jtaChat.append(jtaSay.getText() + "\n\n");
                                        //向服务器发送聊天信息
                                        OutputStream out = Client.s.getOutputStream();
                                        out.write(("Chat/" + Client.uidReceiver.toString() + "/" + jtaSay.getText()).getBytes());
                                    }
                                }
                                catch(Exception e){}
                                finally
                                {
                                    //文本输入框清除
                                    jtaSay.setText("");
                                }
                            }
                        }
                );
        //添加清屏按钮的响应事件
        btnClear.addActionListener
                (
                        new ActionListener()
                        {
                            @Override
                            public void actionPerformed(ActionEvent event)
                            {
                                //聊天框清屏
                                jtaChat.setText("");
                            }
                        }
                );
        //添加退出按钮的响应事件
        btnExit.addActionListener
                (
                        new ActionListener()
                        {
                            @Override
                            public void actionPerformed(ActionEvent event)
                            {
                                try
                                {
                                    //向服务器发送退出信息
                                    OutputStream out = Client.s.getOutputStream();
                                    out.write("Exit/".getBytes());
                                    //退出
                                    System.exit(0);
                                }
                                catch(Exception e){}
                            }
                        }
                );
        //添加在线列表项被鼠标选中的相应事件
        jtbOnline.addMouseListener
                (
                        new MouseListener()
                        {
                            @Override
                            public void mouseClicked(MouseEvent event)
                            {
                                //取得在线列表的数据模型
                                DefaultTableModel tbm = (DefaultTableModel) jtbOnline.getModel();
                                //提取鼠标选中的行作为消息目标，最少一个人，最多全体在线者接收消息
                                int[] selectedIndex = jtbOnline.getSelectedRows();
                                //将所有消息目标的uid拼接成一个字符串, 以逗号分隔
                                Client.uidReceiver = new StringBuilder("");
                                for(int i = 0; i < selectedIndex.length; i++)
                                {
                                    Client.uidReceiver.append((String) tbm.getValueAt(selectedIndex[i], 0));
                                    Client.uidReceiver.append(":");
                                    Client.uidReceiver.append((String) tbm.getValueAt(selectedIndex[i], 1));
                                    if(i != selectedIndex.length - 1)
                                        Client.uidReceiver.append(",");
                                }
                                lblReceiver.setText("发给：" + Client.uidReceiver.toString());
                            }
                            @Override
                            public void mousePressed(MouseEvent event){};
                            @Override
                            public void mouseReleased(MouseEvent event){};
                            @Override
                            public void mouseEntered(MouseEvent event){};
                            @Override
                            public void mouseExited(MouseEvent event){};
                        }
                );
    }
}
