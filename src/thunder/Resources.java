package thunder;

import java.lang.reflect.InvocationTargetException;

public class Resources {
	public String TOO_MUCH_THREADS_WARNING() {
		return "当使用较多的连接线程数时请注意，有些服务器对单一客户端所能使用的最大连接数做了限制。当连接数较多时，"
                + "服务器可能会将您的电脑列入黑名单，尤其是您同时下载多个文件且对于每个文件都使用了较多的连接数。\r\n\r\n" 
                + "此外，对于某些类型的连接或者对于性能较低的电脑、路由器，当增加连接数时下载速度反而会降低。 ";
	}

	public static Object getResource(String resourceName) {
		try {
			return Resources.class.getMethod(resourceName).invoke(Resources.class.newInstance());
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException | InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
