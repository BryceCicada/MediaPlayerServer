package com.we7.player.mediaplayerserver;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;

import org.apache.commons.io.IOUtils;

import android.app.IntentService;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class MainService extends IntentService {

  private static final String TAG = MainService.class.getSimpleName();

  private static final String INTENT_STRING_PREFIX = MainService.class.getPackage().toString();
  private static final String CRLF = "\r\n";

  public static final class Action {
    public static final String CONTROL = INTENT_STRING_PREFIX + "." + TAG + ".action.CONTROL";
    public static final String EVENT = INTENT_STRING_PREFIX + "." + TAG + ".action.EVENT";
  }

  public static final class Category {
    public static final String START = INTENT_STRING_PREFIX + "." + TAG + ".category.START";
    public static final String STOP = INTENT_STRING_PREFIX + "." + TAG + ".category.STOP";

    public static final String STARTED = INTENT_STRING_PREFIX + "." + TAG + ".category.STARTED";
    public static final String STOPPED = INTENT_STRING_PREFIX + "." + TAG + ".category.STOPPED";
  }

  public static final class Extras {
    protected static final String SERVER_URL = INTENT_STRING_PREFIX + "." + TAG + ".extra.SERVER_URL";
  }

  public MainService() {
    super(MainService.class.getSimpleName());
  }

  @Override
  protected void onHandleIntent(final Intent intent) {
    if (Action.CONTROL.equals(intent.getAction())) {
      control(intent);
    }
  }

  private void control(final Intent intent) {
    for (String category : intent.getCategories()) {
      if (Category.START.equals(category)) {
        start(intent);
      } else if (Category.STOP.equals(category)) {
        stop();
      }
    }
  }

  private void stop() {
    Log.d(TAG, "stop >>>");
    sendBroadcast(Action.EVENT, Category.STOPPED);
    Log.d(TAG, "stop <<<");
  }

  private void start(final Intent intent) {
    Log.d(TAG, "start >>> (" + intent + ")");

    Random r = new Random();

    int port = 1024 + r.nextInt(100);

    try {
      ServerSocket serverSocket = new ServerSocket( port );

      InetAddress ia = getIPv4Address(getNetworkInterface());
      if (ia != null) {

        String u = "http://" + ia.toString().substring(1) + ":" + serverSocket.getLocalPort() + "/foo";
        Log.d(TAG, "ServerSocket created at " + u);
        Bundle b = new Bundle();
        b.putString(Extras.SERVER_URL, u);
        sendBroadcast(Action.EVENT, Category.STARTED, b);

        Socket conn = serverSocket.accept();

        Log.d(TAG, "Connection from " + conn.getInetAddress());

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        while (true) {
          String s = br.readLine();
          if ("".equals(s)) {
            break;
          } else {
            Log.d(TAG, s);
          }
        }

        OutputStream out = new BufferedOutputStream(conn.getOutputStream());

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
        bw.write("HTTP/1.1 200 OK" + CRLF);
        bw.write(("Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ").format(new Date())) + CRLF);
        bw.write("Server: My Dicky Server" + CRLF);
        bw.write("Last-Modified: 23 Nov 2011 16:50:22 GMT" + CRLF);
        bw.write("Content-Length: 2461361" + CRLF);
        bw.write("Content-Type: audio/mpeg" + CRLF);
        bw.write(CRLF);
        bw.flush();

        Resources resources = getResources();
        int id = resources.getIdentifier(MainService.class.getPackage().getName() + ":raw/test", null, null);
        InputStream rin = resources.openRawResource(id);
        IOUtils.copy(rin, out);
      }

    } catch (IOException e) {
      Log.d(TAG, "", e);
      stop();
    }


    Log.d(TAG, "start <<<");
  }

  private InetAddress getIPv4Address(final NetworkInterface ni) {
    InetAddress r = null;
    Enumeration<InetAddress> e = ni.getInetAddresses();
    while (e.hasMoreElements()) {
      InetAddress i = e.nextElement();
      if (i.getAddress().length == 4) {
        r = i;
        break;
      }
    }
    return r;

  }

  private NetworkInterface getNetworkInterface() throws SocketException {
    NetworkInterface r = null;
    Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
    while (e.hasMoreElements()) {
      NetworkInterface i = e.nextElement();
      if (i.getName().startsWith("wlan")) {
        r = i;
        break;
      }
    }
    return r;
  }

  private void sendBroadcast(final String action, final String category) {
    sendBroadcast(action, category, null);
  }

  private void sendBroadcast(final String action, final String category, final Bundle data) {
    sendBroadcast(action, category, data, null);
  }

  private void sendBroadcast(final String action, final String category, final Bundle extras, final Uri uri) {
    Intent i = new Intent();
    i.setAction(action);
    i.addCategory(category);
    if (extras != null) {
      i.putExtras(extras);
    }
    if (uri != null) {
      i.setData(uri);
    }
    Log.d(TAG, "Broadcasting " + i);
    LocalBroadcastManager.getInstance(this).sendBroadcast(i);
  }
}
