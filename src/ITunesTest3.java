import com4j.ComThread;
import com4jitunes.IiTunes;

import javax.swing.*;
import java.awt.event.*;
import java.awt.BorderLayout;

public class ITunesTest3 extends JFrame {

	private ITunesThread iTunesThread;

    public ITunesTest3() {
		JButton btnExit = new JButton("Exit");
		btnExit.addActionListener(new ExitListener());

		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(btnExit, BorderLayout.CENTER);
		this.addWindowListener(new ExitListener());
		this.pack();

		iTunesThread = new ITunesThread();
		iTunesThread.start();
	}

    public void doExit() {
		iTunesThread.quit = true;
		while (!iTunesThread.finished) {
			synchronized(this) {
				try {
					System.out.println("Waiting for the ITunes to finish");
					wait(100);
				} catch (InterruptedException e) {}
			}
		}
		System.exit(0);
	}

	private class ExitListener implements ActionListener, WindowListener {
		public void actionPerformed(ActionEvent evt) {
			doExit();
		}

		public void windowClosing(WindowEvent e) {
			doExit();
		}

		public void windowActivated(WindowEvent e) {}
		public void windowClosed(WindowEvent e) {}
		public void windowDeactivated(WindowEvent e) {}
		public void windowDeiconified(WindowEvent e) {}
		public void windowIconified(WindowEvent e) {}
		public void windowOpened(WindowEvent e) {}
	}

	private class ITunesThread extends Thread {
		public boolean quit = false;
		public boolean finished = false;

		public void run () {
            IiTunes it = com4jitunes.ClassFactory.createiTunesApp();

//			ComThread.InitMTA(true);
//			ActiveXComponent iTunesCom = new ActiveXComponent("iTunes.Application");

//			Dispatch iTunesController = iTunesCom.getObject();
//			DispatchEvents events = new DispatchEvents(iTunesController, new ITunesEvents());

			while (!quit) {
				try {
					sleep(100);
				} catch (InterruptedException e) {}
			}

//			ComThread.Release();

			finished = true;
			System.out.println("ITunes has finished");
		}
	}

	private class ITunesEvents {
		//public void OnPlayerPlayEvent(Variant[] args) {
//			System.out.println("OnPlayerPlayEvent");
		//}
	}

	public static void main(String[] args) {
		ITunesTest3 app = new ITunesTest3();
		app.setVisible(true);
	}
}