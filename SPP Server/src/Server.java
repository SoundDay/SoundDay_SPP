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
 * 참고
 * https://webnautes.tistory.com/849
 */

public class Server {

	public static void main(String[] args) {

		log("Local Bluetooth device...\n");

		// 안드로이드 디바이스
		LocalDevice local = null;
		try {
			local = LocalDevice.getLocalDevice();
		} catch (BluetoothStateException e2) {

		}

		// 안드로이드 디바이스 정보(블루투스 주소, 이름)
		log("address: " + local.getBluetoothAddress());
		log("name: " + local.getFriendlyName());

		// 서버 스레드
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

	// 통신 됐는지 안됐는지 알려주는 애
	private StreamConnectionNotifier mStreamConnectionNotifier = null;

	// 통신 시 사용되는 객체
	private StreamConnection mStreamConnection = null;

	// 스레드를 돌면서 통신이 됐는지 본다.
	@Override
	public void run() {

		try {
			// 현재 자바로 작성한 서버를 연다.
			mStreamConnectionNotifier = (StreamConnectionNotifier) Connector.open(CONNECTION_URL_FOR_SPP);

			log("Opened connection successful.");
		} catch (IOException e) {

			log("Could not open connection: " + e.getMessage());
			return;
		}

		log("Server is now running.");

		while (true) {
			// 안드로이드에서 연결할 때까지 기다림
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
		
		//created를 위함
		int count = 0;
		private static final String OutputPath = "C:/Users/zumdahl/Desktop/Sound_day/voice_recorder/";
		
		
		//사용자
//		private static final String OutputPath = "C:/Users/zumdahl/Desktop/Sound_day/voice_recorder/Input.txt";
	
		//AI 답변
		private static final String InputPath = "C:/Users/zumdahl/Desktop/Sound_day/text_folder/ai.txt";
		
		// 데이터 입출력(데이터를 외부에서 읽고(InputStream) 다시 외부로 출력(OutputStream)
		private InputStream mInputStream = null;
		private OutputStream mOutputStream = null;

		// mRemoteDeviceString : 컴퓨터 이름
		private String mRemoteDeviceString = null;
		private StreamConnection mStreamConnection = null;

		Receiver(StreamConnection streamConnection) {
			// 외부 프로그램과 연결됐는지
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
				// 안드로이드 정보
				RemoteDevice remoteDevice = RemoteDevice.getRemoteDevice(mStreamConnection);

				mRemoteDeviceString = remoteDevice.getBluetoothAddress();

				log("Remote device");
				log("address: " + mRemoteDeviceString);

				//여기다가 넣어보자(AI 답변)
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
				// 연결 됨

				// BufferedReader 버퍼를 이용해서 읽고 쓰는 함수
				// 여기서는 안드로이드가 보내는 mInputStream에서 받은걸 읽음
				Reader mReader = new BufferedReader(
						new InputStreamReader(mInputStream, Charset.forName(StandardCharsets.UTF_8.name())));

				boolean isDisconnected = false;

				while (true) {

					log("ready");

					// StringBuilder 문자열을 더할 때,기존의 데이터에 더하는 방식이라
					// String보다 성능이 좋다
					StringBuilder stringBuilder = new StringBuilder();
					int c = 0;

					// mReader의 글자하나하나 읽어가며 stringBuilder에 넣어준다.
					while ('\n' != (char) (c = mReader.read())) {

						if (c == -1) {
							// 안드로이드가 나감
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

					// stringBuilder를 recvMessage에 넣어준다.
					// String으로 바꾸기 위함
					// recvMessage : 안드로이드가 보내준거 들어가있다.
					String recvMessage = stringBuilder.toString();

					// 안드로이드한테 받은거 Output.txt으로 보내줌
					// 외부에 출력을 전달하기 위해 outputStream에 넣어줌
					

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

					// 서버가 안드로이드에게 보냄
//					WatchDogs();
				}
			} catch (IOException e) {

				log("Receiver closed" + e.getMessage());
			}
		}

		// 컴퓨터가 안드로이드에게 메세지를 보냄
		void WatchDogs() throws IOException {
			// watcher service
			WatchService watchService = FileSystems.getDefault().newWatchService();

			String dir = "C:/Users/zumdahl/Desktop/Sound_day/text_folder";
			// 경로 생성
			Path path = Paths.get(dir);
			// 해당 디렉토리 경로에 와치서비스와 이벤트 등록
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

					List<WatchEvent<?>> events = watchKey.pollEvents();// 이벤트들을 가져옴
					for (WatchEvent<?> event : events) {
						// 이벤트 종류
						Kind<?> kind = event.kind();
						// 경로
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
			// send 함수
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
				log("파일을 찾을 수 없습니다");
				e.getStackTrace();
			} catch (IOException e) {
				log("에러");
				e.getStackTrace();
			}

			String message = stringBuilder.toString();

			PrintWriter printWriter = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(mOutputStream, Charset.forName(StandardCharsets.UTF_8.name()))));

			printWriter.write(message + "\n");
			printWriter.flush();

			log("Me : " + message);
			// 컴퓨터 쪽에서 보낸거 log 찍음
		}

	}

	private static void log(String msg) {
		System.out.println("[" + (new Date()) + "] " + msg);
	}

}