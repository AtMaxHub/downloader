package cn.go.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 参考：https://blog.csdn.net/jjzhoulong/article/details/78622003
 */
public class M3U8 {
	private String basepath;
	private List<Ts> tsList = new ArrayList<>();
	private long startTime;// 开始时间
	private long endTime;// 结束时间
	private long startDownloadTime;// 开始下载时间
	private long endDownloadTime;// 结束下载时间
	private String referer;
	private  String AES_KEY_URL ;
	private  byte[] AES_KEY ;
	private  String AES_IV ;

    public String getAES_KEY_URL() {
        return AES_KEY_URL;
    }

    public void setAES_KEY_URL(String AES_KEY_URL) {
        this.AES_KEY_URL = AES_KEY_URL;
    }

    public byte[] getAES_KEY() {
        return AES_KEY;
    }

    public void setAES_KEY(byte[] AES_KEY) {
        this.AES_KEY = AES_KEY;
    }

    public String getAES_IV() {
        return AES_IV;
    }

    public void setAES_IV(String AES_IV) {
        this.AES_IV = AES_IV;
    }

    private List<String> urlList = new LinkedList<>();

	public List<String> getUrlList() {
		return urlList;
	}

	public void setUrlList(List<String> urlList) {
		this.urlList = urlList;
	}

	public void addUrl(String url) {
		this.urlList.add(url);
	}

	public String getReferer() {
		return referer;
	}

	public void setReferer(String referer) {
		this.referer = referer;
	}

	public String getBasepath() {
		return basepath;
	}

	public void setBasepath(String basepath) {
		this.basepath = basepath;
	}

	public List<Ts> getTsList() {
		return tsList;
	}

	public void setTsList(List<Ts> tsList) {
		this.tsList = tsList;
	}

	public void addTs(Ts ts) {
		this.tsList.add(ts);
	}

	public long getStartDownloadTime() {
		return startDownloadTime;
	}

	public void setStartDownloadTime(long startDownloadTime) {
		this.startDownloadTime = startDownloadTime;
	}

	public long getEndDownloadTime() {
		return endDownloadTime;
	}

	public void setEndDownloadTime(long endDownloadTime) {
		this.endDownloadTime = endDownloadTime;
	}

	/**
	 * 获取开始时间
	 *
	 * @return
	 */
	public long getStartTime() {
		if (tsList.size() > 0) {
			Collections.sort(tsList);
			startTime = tsList.get(0).getLongDate();
			return startTime;
		}
		return 0;
	}

	/**
	 * 获取结束时间(加上了最后一段时间的持续时间)
	 *
	 * @return
	 */
	public long getEndTime() {
		if (tsList.size() > 0) {
			Ts m3U8Ts = tsList.get(tsList.size() - 1);
			endTime = m3U8Ts.getLongDate() + (long) (m3U8Ts.getSeconds() * 1000);
			return endTime;
		}
		return 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("basepath: " + basepath);
		for (Ts ts : tsList) {
			sb.append("\nts_file_name = " + ts);
		}
		sb.append("\n\nstartTime = " + startTime);
		sb.append("\n\nendTime = " + endTime);
		sb.append("\n\nstartDownloadTime = " + startDownloadTime);
		sb.append("\n\nendDownloadTime = " + endDownloadTime);
		return sb.toString();
	}
	public static class Ts implements Comparable<Ts> {
		private String url;
		private String file;
		private float seconds;

		public Ts(String url, String file, float seconds) {
			this.url = url;
			this.file = file;
			this.seconds = seconds;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getFile() {
			return file;
		}

		public void setFile(String file) {
			this.file = file;
		}

		public float getSeconds() {
			return seconds;
		}

		public void setSeconds(float seconds) {
			this.seconds = seconds;
		}

		@Override
		public String toString() {
			return file + " (" + seconds + "sec)";
		}

		/**
		 * 获取时间
		 */
		public long getLongDate() {
			try {
				return Long.parseLong(file.substring(0, file.lastIndexOf(".")));
			} catch (Exception e) {
				return 0;
			}
		}

		@Override
		public int compareTo(Ts o) {
			return file.compareTo(o.file);
		}
	}
}

