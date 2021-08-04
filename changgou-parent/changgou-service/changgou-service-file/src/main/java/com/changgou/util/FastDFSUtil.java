package com.changgou.util;

import com.changgou.file.FastDFSFile;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;

import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FastDFSUtil {
    /***
     * 加载tracker链接信息
     */
    static {
        try {
            //获取tracker的配置文件fdfs_client.conf的位置
            String filePath = new ClassPathResource("fdfs_client.conf").getPath();
            //加载tracker配置信息
            ClientGlobal.init(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //优化
    //获取tracker
    public static TrackerServer getTrackerServer() throws Exception{
        //创建一个tracker访问的客户端对象trackerClient
        TrackerClient trackerClient = new TrackerClient();

        //通过trackerClient访问trackerServer服务，获取链接信息
        TrackerServer trackerServer = trackerClient.getConnection();
        return trackerServer;
    }



    public static String[] upload(FastDFSFile fastDFSFile)throws Exception{
        //附加参数
        NameValuePair[] meta_list = new NameValuePair[1];
        meta_list[0] = new NameValuePair("author",fastDFSFile.getAuthor());

        //获取trackerServer
        TrackerServer trackerServer = getTrackerServer();

        //通过trackerServer的连接信息可以获取storage的链接信息，创建storageClient对象存储storage的链接信息
        StorageClient storageClient = new StorageClient(trackerServer, null);

        //通过storageClient访问storage,实现文件上传，并且获取文件上传后的存储信息
        /**
         * 1.上传的文件字节数组
         * 2.文件的扩展名 jpg
         * 3.附加参数 比如：拍摄地址：北京
         *
         * uploads[]
         *      uploads[0]：文件上传所存储的storage的组的名字
         *      uploads[1]:文件存储到storage上的文件名字  M00/02/44/1.jpg
         */
        String[] uploads = storageClient.upload_file(fastDFSFile.getContent(), fastDFSFile.getExt(), meta_list);
        return uploads;
    }

    /**
     * 获取文件信息
     * @param groupName
     * @param remoteFileName
     * @return
     * @throws Exception
     */
    public static FileInfo getFile(String groupName,String remoteFileName) throws Exception{
        //获取trackerServer
        TrackerServer trackerServer = getTrackerServer();

        //通过trackerServer的连接信息可以获取storage的链接信息，创建storageClient对象存储storage的链接信息
        StorageClient storageClient = new StorageClient(trackerServer, null);

        //获取文件信息
        return storageClient.get_file_info(groupName,remoteFileName);
    }

    /**
     * 文件下载
     * @param groupName
     * @param remoteFileName
     * @return
     * @throws Exception
     */
    public static InputStream downloadFile(String groupName,String remoteFileName) throws Exception{
        //获取trackerServer
        TrackerServer trackerServer = getTrackerServer();

        //通过trackerServer的连接信息可以获取storage的链接信息，创建storageClient对象存储storage的链接信息
        StorageClient storageClient = new StorageClient(trackerServer, null);

        //文件下载
        byte[] buffer = storageClient.download_file(groupName, remoteFileName);
        // 可以将字节数组转化为输入流 。
        return new ByteArrayInputStream(buffer);
    }

    /**
     * 删除文件
     * @param groupName
     * @param remoteFileName
     * @throws Exception
     */
    public static void delete(String groupName,String remoteFileName) throws Exception{
        //获取trackerServer
        TrackerServer trackerServer = getTrackerServer();

        //通过trackerServer的连接信息可以获取storage的链接信息，创建storageClient对象存储storage的链接信息
        StorageClient storageClient = new StorageClient(trackerServer, null);

        //删除文件
        storageClient.delete_file(groupName,remoteFileName);
    }

    /**
     * 获取storage信息
     * @return
     * @throws Exception
     */
    public static StorageServer getStorage()throws Exception{
        //创建一个tracker访问的客户端对象trackerClient
        TrackerClient trackerClient = new TrackerClient();

        //通过trackerClient访问trackerServer服务，获取链接信息
        TrackerServer trackerServer = trackerClient.getConnection();

        //获取storage信息
        return trackerClient.getStoreStorage(trackerServer);
    }

    /**
     * 获取storage的ip和端口信息
     * @param groupName
     * @param remoteFileName
     * @return
     * @throws Exception
     */
    public static ServerInfo[] getServerInfo(String groupName,String remoteFileName) throws Exception{
        //创建一个tracker访问的客户端对象trackerClient
        TrackerClient trackerClient = new TrackerClient();

        //通过trackerClient访问trackerServer服务，获取链接信息
        TrackerServer trackerServer = trackerClient.getConnection();

        //获取storage的ip和端口信息
        return trackerClient.getFetchStorages(trackerServer,groupName,remoteFileName);
    }

    /**
     *
     * @return
     * @throws Exception
     */
    public static String getTrackerInfo() throws Exception{
        //获取trackerServer
        TrackerServer trackerServer = getTrackerServer();;

        //Tracker的IP，HTTP端口
        String ip = trackerServer.getInetSocketAddress().getHostName();
        int tracker_http_port = ClientGlobal.getG_tracker_http_port();//8080

        String url = "http://"+ip+":"+tracker_http_port;
        return url;
    }



    /**
     * 测试
     * @param args
     */
    public static void main(String[] args) throws Exception{

        //测试获取文件信息
//       FileInfo fileInfo = getFile("group1","M00/00/00/wKjThF0DBzaAP23MAAXz2mMp9oM26.jpeg");
//        System.out.println(fileInfo.getSourceIpAddr());
//        System.out.println(fileInfo.getFileSize());

        //测试下载文件
        //文件下载
//        InputStream is = downloadFile("group1", "M00/00/00/wKjThGC_O2mAUs9kAAlE0yPoKKg693.jpg");
        //将文件写入到本地磁盘
//        FileOutputStream os= new FileOutputStream("D:/1.jpg");

        //定义一个缓冲区
//        byte[] buffer = new byte[1024];
//        while (is.read(buffer)!=-1){
//            os.write(buffer);
//        }
//        os.flush();
//        os.close();
//        is.close();


        //删除文件
        //delete("group1", "M00/00/00/wKjThGC_O2mAUs9kAAlE0yPoKKg693.jpg");


        //获取storage信息
//        StorageServer storageServer = getStorage();
//        System.out.println(storageServer.getStorePathIndex());
//        System.out.println(storageServer.getInetSocketAddress().getHostString()); //ip信息

        //获取storage组的ip和端口信息
//        ServerInfo[] group1s = getServerInfo("group1", "M00/00/00/wKjThGC_NpuAfXcrAA7ZH6X2s9Q191.jpg");
//        for (ServerInfo group1 : group1s) {
//            System.out.println(group1.getIpAddr());
//            System.out.println(group1.getPort());
//        }


        //获取Tracker的信息
        System.out.println(getTrackerInfo());


    }







}
