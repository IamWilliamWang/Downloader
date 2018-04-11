package thunder;

import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

public class DownloadUtil {
    // 定义下载路径
    private String path;
    // 指定所下载的文件的保存位置
    private String targetFile;
    // 定义下载线程的数量
    private int threadNum;
    // 定义下载线程的对象
    private DownloadThread[] threads;
    // 下载文件的总大小
    private int fileSize;

    private RandomAccessFile file;
    
    /**
     * 定义下载对象
     * @param path 下载路径
     * @param targetFile 所下载的文件的保存位置
     * @param threadNum 下载线程的数量
     */
    public DownloadUtil(String path, String targetFile, int threadNum) {
        this.path = path;
        this.targetFile = targetFile;
        this.threadNum = threadNum;
        threads = new DownloadThread[threadNum];
        this.targetFile = targetFile;
    }

    /**
     * 开始进行多线程下载
     */
    public void downLoad() {
        URL url;
		try {
			url = new URL(path);
		
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setConnectTimeout(5 * 1000);
	        // 设置 URL 请求的方法， GET POST HEAD OPTIONS PUT DELETE TRACE 以上方法之一是合法的，具体取决于协议的限制。
	        conn.setRequestMethod("GET");
	
	        // 设置一般请求属性。如果已存在具有该关键字的属性，则用新值改写其值。
	        // conn.setRequestProperty("Accpt",
	        // "image/gif,image/jpeg,image/pjpeg,image/pjpeg, "
	        // + "application/x-shockwave-flash, application/xaml+xml, "
	        // + "application/vnd.ms-xpsdocument, application/x-ms-xbap"
	        // + "application/x-ms-application,application/vnd.ms-excel"
	        // + "application/vnd.ms-powerpoint, application/msword,*/*");
	        conn.setRequestProperty("Accept-Language", "zh_CN");
	        conn.setRequestProperty("Charset", "UTF-8");
	        conn.setRequestProperty("Connection", "Keep-Alive");
	
	        // 得到文件大小
	        fileSize = conn.getContentLength();
	        conn.disconnect();
	        int currentPartSize = fileSize / threadNum + 1;
	        file = new RandomAccessFile(targetFile, "rw");
	        // 设置本地文件大小
	        file.setLength(fileSize);
	        file.close();
	        for (int i = 0; i < threadNum; i++) {
	            // 计算每个线程的下载位置
	            int startPos = i * currentPartSize;
	            // 每个线程使用一个RandomAccessFile进行下载
	            RandomAccessFile currentPart = new RandomAccessFile(targetFile, "rw");
	            // 定位该线程的下载位置
	            currentPart.seek(startPos);
	            // 创建下载线程
	            if (i == threadNum - 1)
	                threads[i] = new DownloadThread(startPos, fileSize - startPos, currentPart);
	            else
	                threads[i] = new DownloadThread(startPos, currentPartSize, currentPart);
	            threads[i].start();
	        }
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
    }

    /**
     * 打断所有的线程
     */
    public void destroy() {
        for(Thread th : threads) {
        	th.interrupt();
        }
        try {
			Thread.sleep(80);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				file.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }

    public boolean closeFile() {
    	try {
			this.file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
    	return true;
    }
    
    /**
     * 获得下载进度，返回0-1之间的数字
     * @return 下载进度，用[0,1]的数字表示
     */
    public double getCompleteRate() {
        int sumSize = 0;
        for (int i = 0; i < threadNum; i++) {
            sumSize += threads[i].length;
        }
        double rate = sumSize * 1.0 / fileSize;
        if (rate > 1)
            rate = 1;
        return rate;
    }

    /**
     * 获得下载文件总字节数
     * @return 需要下载文件总字节数
     */
    public int getDownloadFileSize() {
        return this.fileSize;
    }
    
    /**
     * 获得当前下载文件的实例
     * @return 正在下载的文件
     */
    public RandomAccessFile getFile() {
    	return this.file;
    }

    /**
     * 内部下载线程类
     */
    private class DownloadThread extends Thread {
        // 当前线程的下载位置
        private int startPos;
        // 定义当前线程负责下载的文件大小
        private int currentPartSize;
        // 当前线程需要下载的文件块,此类的实例支持对随机访问文件的读取和写入。
        private RandomAccessFile currentPart;
        // 定义该线程已下载的字节数
        public int length;

        private DownloadThread(int startPos, int currentPartSize, RandomAccessFile currentPart) {
            this.startPos = startPos;
            this.currentPartSize = currentPartSize;
            this.currentPart = currentPart;
        }

        public void run() {
            try {
                length = 0; //重置已下载长度

                currentPart.seek(startPos); //文件内的下载起点

                URL url = new URL(path);
                // url.openConnection():返回一个 URLConnection 对象，它表示到 URL 所引用的远程对象的连接。
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5 * 1000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Range", "bytes=" + startPos + "-" + (startPos + currentPartSize - 1));

                // conn.setRequestProperty("Accpt",
                // "image/gif,image/jpeg,image/pjpeg,image/pjpeg, "
                // + "application/x-shockwave-flash, application/xaml+xml, "
                // + "application/vnd.ms-xpsdocument, application/x-ms-xbap"
                // + "application/x-ms-application,application/vnd.ms-excel"
                // + "application/vnd.ms-powerpoint, application/msword,*/*");
                conn.setRequestProperty("Accept-Language", "zh_CN");
                conn.setRequestProperty("Charset", "UTF-8");
                conn.setReadTimeout(30 * 1000);
                InputStream inputStream = conn.getInputStream();
                // inputStream.skip(n);跳过和丢弃此输入流中数据的 n 个字节
                // inputStream.skip(this.startPos);
                byte[] buffer = new byte[1024];
                int hasRead = 0;
                // 读取网络数据写入本地
                while ( length < currentPartSize && (hasRead = inputStream.read(buffer)) != -1) {
                	int writeBytesCount;
                	if(length+hasRead<=currentPartSize)
                		writeBytesCount=hasRead;
                	else 
                		writeBytesCount=currentPartSize-length;
                    currentPart.write(buffer, 0, writeBytesCount);
                    length += writeBytesCount;
                    
                    if(Thread.currentThread().isInterrupted())
                    	break;
//                    System.out.println("Download thread.");
                }
                currentPart.close();
                inputStream.close();
            } catch (java.net.SocketTimeoutException socketE) { //当遇到网络阻塞而丢包，则重新下载该片段
                run();
            } catch (Exception e) {
                run();
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        StringBuilder str = new StringBuilder();
        int i = 1;
        for (DownloadThread thread : threads) {
            str.append(String.format("线程 %d: %.1f%%\n", i++, 100.0 * thread.length / thread.currentPartSize));
        }
        // str.append(String.format("总进度: %.1f%%\n", 100 * getCompleteRate()));
        return str.toString();
    }
}