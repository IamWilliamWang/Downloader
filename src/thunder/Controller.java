package thunder;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import java.io.File;
import java.io.IOException;

public class Controller {
	@FXML
	private javafx.scene.control.TextField downloadUrlTextField, filenameTextField;
	@FXML
	private ComboBox<String> threadsCountCombobox;
	@FXML
	private TextArea logTextArea;
	@FXML
	private ProgressBar progressBar;
	@FXML
	private Label progressBarText, downloadSpeed, totalTime;

	DownloadUtil download; // 保存下载类实例
	Thread refreshScreenThread;

	private int time = 0; // 下载总时长

	/**
	 * 粘贴键点击事件
	 * 
	 * @param actionEvent JavaFX点击事件
	 */
	public void paste_onClick(ActionEvent actionEvent) {
		downloadUrlTextField.setText(SystemClipboardUtil.getSysClipboardText());
		this.filenameTextField
				.setText(downloadUrlTextField.getText().substring(downloadUrlTextField.getText().lastIndexOf("/") + 1));
	}

	/**
	 * 选择文件点击事件
	 * 
	 * @param actionEvent JavaFX点击事件
	 */
	public void selectFile_onClick(ActionEvent actionEvent) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("选择要保存的文件位置");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("所有文件", "*.*"));
		File saveFile = fileChooser.showSaveDialog(Main.PrimaryStage());
		if (saveFile != null)
			this.filenameTextField.setText(saveFile.getAbsolutePath());
	}

	/**
	 * 开始下载点击事件
	 * 
	 * @param actionEvent JavaFX点击事件
	 */
	public void startDownload_onClick(ActionEvent actionEvent) {
		if (this.threadsCountCombobox.getValue() == null) { // 下拉框是空的时候
			Alert alertDialog = new Alert(Alert.AlertType.WARNING, "请选择线程数量");
			alertDialog.setTitle("警告");
			alertDialog.setHeaderText("");
			alertDialog.showAndWait();
			return;
		}
		if (this.filenameTextField.getText().isEmpty() || this.downloadUrlTextField.getText().isEmpty()) { // 两个文本框是空的时候
			Alert alertDialog = new Alert(Alert.AlertType.WARNING, "请填写上面的空白处");
			alertDialog.setTitle("警告");
			alertDialog.setHeaderText("");
			alertDialog.showAndWait();
			return;
		}

		String downloadUrl = this.downloadUrlTextField.getText();
		String fileName = this.filenameTextField.getText();
		int threadNum = 1;
		try {
			threadNum = Integer.parseInt(this.threadsCountCombobox.getValue());
			if (threadNum < 1)
				throw new Exception();
		} catch (Exception formatException) { // 线程数不是正整数都会报错
			Alert alertDialog = new Alert(Alert.AlertType.WARNING, "线程数必须是正整数");
			alertDialog.setTitle("警告");
			alertDialog.setHeaderText("");
			alertDialog.showAndWait();
		}
		final DownloadUtil downUtil = new DownloadUtil(downloadUrl, fileName, threadNum); // 创建下载对象
		download = downUtil; // 备份

		downUtil.downLoad();
		Thread refreshScreenThread = new Thread(() -> {

			while (downUtil.getCompleteRate() < 1) {
				setDownloadRate(downUtil.getCompleteRate());

				update(this, downUtil); // 每隔一秒，更新屏幕上的所有信息

				try {// 每隔1秒查询一次任务
					Thread.sleep(1000);
				} catch (InterruptedException ie) {
					downUtil.closeFile();
					return;
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
					return;
				}
			}

			// 由于不能打扰GUI线程的执行，必须调用runLater方法才能执行修改操作
			Platform.runLater(() -> {
				Controller.this.logTextArea.setText("下载已完成! 用时：" + this.totalTime.getText() + ", 平均速度: "
						+ (int) (1.0 * this.download.getDownloadFileSize() / 1024 / time) + " kb/s");
				Controller.this.progressBar.setProgress(1);
				Controller.this.progressBarText.setText("100%");
				Controller.this.downloadSpeed.setText("0 kb/s");
			});
			
			downUtil.closeFile();

		});
		refreshScreenThread.start();
		this.refreshScreenThread = refreshScreenThread;

		// 清空数据
		this.time = 0;
		this.downloadRateArray[0] = downloadRateArray[1] = 0;
	}

	/**
	 * 更新屏幕上的所有信息
	 * 
	 * @param controller 传入的控制器名称
	 * @param downUtil 下载类
	 */
	private void update(Controller controller, DownloadUtil downUtil) {
		// 由于不能打扰GUI线程的执行，必须调用runLater方法才能执行修改操作
		Platform.runLater(() -> {
			Controller.this.logTextArea.setText(downUtil.toString());
			Controller.this.totalTime.setText(getTotalTimeStr(time++));
			Controller.this.progressBar.setProgress(getDownloadRate());
			Controller.this.progressBarText.setText(String.format("%.0f%%", getDownloadRate() * 100));
			Controller.this.downloadSpeed.setText(
					(int) ((getDownloadRate() - getBeforeDownloadRate()) * downUtil.getDownloadFileSize() / 1024)
							+ " kb/s");

		});
	}

	/**
	 * 停止下载点击事件
	 * 
	 * @param actionEvent JavaFX点击事件
	 */
	public void stopDownload_onClick(ActionEvent actionEvent) {
		download.destroy();
		refreshScreenThread.interrupt();
		Platform.runLater(() -> {
			// TODO Auto-generated method stub
			Controller.this.progressBar.setProgress(0);
			Controller.this.progressBarText.setText("0%");

		});

	}

	/**
	 * 线程数框内容变化事件
	 * 
	 * @param actionEvent JavaFX点击事件
	 */
	public void threadsCount_TextChanged(ActionEvent actionEvent) {
		if (this.threadsCountCombobox.getValue() == null)
			return;

		if (this.threadsCountCombobox.getValue().equals("其他")) {
			this.threadsCountCombobox.setEditable(true);
			this.threadsCountCombobox.setValue("");
			this.threadsCountCombobox.getItems().remove(this.threadsCountCombobox.getItems().size() - 1);
			return;
		}

		if (Integer.parseInt(this.threadsCountCombobox.getValue()) > 20) {
			Alert alertDialog = new Alert(Alert.AlertType.INFORMATION,
					Resources.getResource("TOO_MUCH_THREADS_WARNING").toString(), ButtonType.OK, ButtonType.CANCEL);
			alertDialog.setTitle("默认最大连接数");
			alertDialog.setHeaderText("");
			alertDialog.showAndWait();
		}
	}

	/**
	 * 根据秒数转换成时长字符串
	 * 
	 * @param timeSec 输入秒数的整形值
	 * @return 返回“时:分:秒”格式的字符串
	 */
	private static String getTotalTimeStr(int timeSec) {
		int hr = 0;
		int mi = 0;
		int se = timeSec;
		for (; se >= 60; se -= 60, mi++)
			;
		for (; mi >= 60; mi -= 60, hr++)
			;
		StringBuilder str = new StringBuilder();
		if (hr != 0)
			str.append(String.format("%02d:%02d:%02d", hr, mi, se));
		else if (mi != 0)
			str.append(String.format("%02d:%02d", mi, se));
		else
			str.append(String.format("%2ds", se));
		return str.toString();
	}

	/* 以下几行是用来记录此次和上次的下载进度的，使用了长度为2的循环队列 */
	private double downloadRateArray[] = new double[2];
	private int pointer = 0;

	private double getDownloadRate() {
		return downloadRateArray[pointer];
	}

	private void setDownloadRate(double downloadRate) {
		pointer = (pointer + 1) % 2;
		downloadRateArray[pointer] = downloadRate;
	}

	private double getBeforeDownloadRate() {
		return downloadRateArray[(pointer + 1) % 2];
	}

}
