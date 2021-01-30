import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Date;
import java.util.List;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

/*
 * ����
 * https://webnautes.tistory.com/849
 */

public class Server {

	public static void main(String[] args) {

		log("Local Bluetooth device...\n");

		// �ȵ���̵� ����̽�
		LocalDevice local = null;
		try {
			local = LocalDevice.getLocalDevice();
		} catch (BluetoothStateException e2) {

		}

		// �ȵ���̵� ����̽� ����(������� �ּ�, �̸�)
		log("address: " + local.getBluetoothAddress());
		log("name: " + local.getFriendlyName());

		// ���� ������
		Runnable r = new ServerRunable();
		Thread thread = new Thread(r);
		thread.start();
	}

	private static void log(String msg) {
		System.out.println("[" + (new Date()) + "] " + msg);
	}

}

class ServerRunable implements Runnable {

	// UUID for SPP
	final UUID uuid = new UUID("0000110100001000800000805F9B34FB", false);

	final String CONNECTION_URL_FOR_SPP = "btspp://localhost:" + uuid + ";name=SPP Server";

	// ��� �ƴ��� �ȵƴ��� �˷��ִ� ��
	private StreamConnectionNotifier mStreamConnectionNotifier = null;

	// ��� �� ���Ǵ� ��ü
	private StreamConnection mStreamConnection = null;

	// �����带 ���鼭 ����� �ƴ��� ����.
	@Override
	public void run() {

		try {
			// ���� �ڹٷ� �ۼ��� ������ ����.
			mStreamConnectionNotifier = (StreamConnectionNotifier) Connector.open(CONNECTION_URL_FOR_SPP);

			log("Opened connection successful.");
		} catch (IOException e) {

			log("Could not open connection: " + e.getMessage());
			return;
		}

		log("Server is now running.");

		while (true) {
			// �ȵ���̵忡�� ������ ������ ��ٸ�
			log("wait for client requests...");

			try {

				mStreamConnection = mStreamConnectionNotifier.acceptAndOpen();
			} catch (IOException e1) {

				log("Could not open connection: " + e1.getMessage());
			}

			new Receiver(mStreamConnection).start();
		}

	}

	class Receiver extends Thread {
		
		//created�� ����
		int count = 0;
		private static final String OutputPath = "C:/Users/zumdahl/Desktop/Sound_day/voice_recorder/";
		
		
		//�����
//		private static final String OutputPath = "C:/Users/zumdahl/Desktop/Sound_day/voice_recorder/Input.txt";
	
		//AI �亯
		private static final String InputPath = "C:/Users/zumdahl/Desktop/Sound_day/text_folder/ai.txt";
		
		// ������ �����(�����͸� �ܺο��� �а�(InputStream) �ٽ� �ܺη� ���(OutputStream)
		private InputStream mInputStream = null;
		private OutputStream mOutputStream = null;

		// mRemoteDeviceString : ��ǻ�� �̸�
		private String mRemoteDeviceString = null;
		private StreamConnection mStreamConnection = null;

		Receiver(StreamConnection streamConnection) {
			// �ܺ� ���α׷��� ����ƴ���
			mStreamConnection = streamConnection;

			try {

				mInputStream = mStreamConnection.openInputStream();
				mOutputStream = mStreamConnection.openOutputStream();

				log("Open streams...");
			} catch (IOException e) {

				log("Couldn't open Stream: " + e.getMessage());

				Thread.currentThread().interrupt();
				return;
			}

			try {
				// �ȵ���̵� ����
				RemoteDevice remoteDevice = RemoteDevice.getRemoteDevice(mStreamConnection);

				mRemoteDeviceString = remoteDevice.getBluetoothAddress();

				log("Remote device");
				log("address: " + mRemoteDeviceString);

				//����ٰ� �־��(AI �亯)
				WatchDogs();
				
			} catch (IOException e1) {

				log("Found device, but couldn't connect to it: " + e1.getMessage());
				return;
			}

			log("Client is connected...");
		}

		@Override
		public void run() {

			try {
				// ���� ��

				// BufferedReader ���۸� �̿��ؼ� �а� ���� �Լ�
				// ���⼭�� �ȵ���̵尡 ������ mInputStream���� ������ ����
				Reader mReader = new BufferedReader(
						new InputStreamReader(mInputStream, Charset.forName(StandardCharsets.UTF_8.name())));

				boolean isDisconnected = false;

				while (true) {

					log("ready");

					// StringBuilder ���ڿ��� ���� ��,������ �����Ϳ� ���ϴ� ����̶�
					// String���� ������ ����
					StringBuilder stringBuilder = new StringBuilder();
					int c = 0;

					// mReader�� �����ϳ��ϳ� �о�� stringBuilder�� �־��ش�.
					while ('\n' != (char) (c = mReader.read())) {

						if (c == -1) {
							// �ȵ���̵尡 ����
							log("Client has been disconnected");

							isDisconnected = true;
							Thread.currentThread().interrupt();
							break;
						}
						//
						stringBuilder.append((char) c);
					}

					if (isDisconnected)
						break;

					// stringBuilder�� recvMessage�� �־��ش�.
					// String���� �ٲٱ� ����
					// recvMessage : �ȵ���̵尡 �����ذ� ���ִ�.
					String recvMessage = stringBuilder.toString();

					// �ȵ���̵����� ������ Output.txt���� ������
					// �ܺο� ����� �����ϱ� ���� outputStream�� �־���
					

					try {
						String real_Out = OutputPath+count+".txt";
								
						count++;
						OutputStream output = new FileOutputStream(real_Out);
						byte[] by = recvMessage.getBytes();
						output.write(by);
						output.close();

					} catch (Exception e) {
						e.getStackTrace();
					}
					log(mRemoteDeviceString + " : " + recvMessage);

					// ������ �ȵ���̵忡�� ����
//					WatchDogs();
				}
			} catch (IOException e) {

				log("Receiver closed" + e.getMessage());
			}
		}

		// ��ǻ�Ͱ� �ȵ���̵忡�� �޼����� ����
		void WatchDogs() throws IOException {
			// watcher service
			WatchService watchService = FileSystems.getDefault().newWatchService();

			String dir = "C:/Users/zumdahl/Desktop/Sound_day/text_folder";
			// ��� ����
			Path path = Paths.get(dir);
			// �ش� ���丮 ��ο� ��ġ���񽺿� �̺�Ʈ ���
			path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.OVERFLOW);

			Thread thread = new Thread(() -> {
				while (true) {
					WatchKey watchKey = null;
					try {
						watchKey = watchService.take();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}

					List<WatchEvent<?>> events = watchKey.pollEvents();// �̺�Ʈ���� ������
					for (WatchEvent<?> event : events) {
						// �̺�Ʈ ����
						Kind<?> kind = event.kind();
						// ���
						Path paths = (Path) event.context();
						System.out.println(paths.toAbsolutePath());// C:\...\...\test.txt
						if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
							System.out.println("created something in directory");
						} else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
							System.out.println("delete something in directory");
						} else if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
							String tmp = paths.getFileName().toString();
							System.out.println(tmp);
							if (tmp.equals("ai.txt")) {
								Send();
								System.out.println("modified something in directory");
							}
							
						} else if (kind.equals(StandardWatchEventKinds.OVERFLOW)) {
							System.out.println("overflow");
						} else {
							System.out.println("hello world");
						}
					}
					if (!watchKey.reset()) {
						try {
							watchService.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			});
			thread.start();
		}

		void Send() {
			// send �Լ�
			StringBuilder stringBuilder = new StringBuilder();
			try {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(new FileInputStream(InputPath), "UTF-8"));
				String line = null;
				if ((line = br.readLine()) != null) {
					stringBuilder.append(line);
				}
				br.close();
			} catch (FileNotFoundException e) {
				log("������ ã�� �� �����ϴ�");
				e.getStackTrace();
			} catch (IOException e) {
				log("����");
				e.getStackTrace();
			}

			String message = stringBuilder.toString();

			PrintWriter printWriter = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(mOutputStream, Charset.forName(StandardCharsets.UTF_8.name()))));

			printWriter.write(message + "\n");
			printWriter.flush();

			log("Me : " + message);
			// ��ǻ�� �ʿ��� ������ log ����
		}

	}

	private static void log(String msg) {
		System.out.println("[" + (new Date()) + "] " + msg);
	}

}