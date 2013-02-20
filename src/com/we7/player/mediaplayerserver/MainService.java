package com.we7.player.mediaplayerserver;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
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

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class MainService extends Service {

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
    public static final String CONNECTED = INTENT_STRING_PREFIX + "." + TAG + ".category.CONNECTED";;
    public static final String REQUEST = INTENT_STRING_PREFIX + "." + TAG + ".category.REQUEST";;
  }

  public static final class Extras {
    public static final String SERVER_URL = INTENT_STRING_PREFIX + "." + TAG + ".extra.SERVER_URL";
    public static final String CONNECTION_URL = INTENT_STRING_PREFIX + "." + TAG + ".extra.CONNECTION_URL";
    public static final String REQUEST_TYPE = INTENT_STRING_PREFIX + "." + TAG + ".extra.REQUEST_TYPE";
  }

  private static enum HttpMethod {
    HEAD("HEAD"),
    GET("GET"),
    UNDEFINED("UNDEFINED");

    private final String mMethodString;

    HttpMethod(final String methodString) {
      mMethodString = methodString;
    }

    public String getMethodString() {
      return mMethodString;
    }

    public static HttpMethod getByMethodString(final String s) {
      HttpMethod r = UNDEFINED;
      for (HttpMethod m : HttpMethod.values()) {
        if (m.getMethodString().equals(s)) {
          r = m;
          break;
        }
      }
      return r;
    }
  }

  @Override
  public int onStartCommand(final Intent intent, final int flags, final int startId) {
    Log.i("LocalService", "Received start id " + startId + ": " + intent);

    if (Action.CONTROL.equals(intent.getAction())) {
      control(intent);
    }

    // We want this service to continue running until it is explicitly
    // stopped, so return sticky.
    return START_STICKY;
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

    ServerSocket serverSocket = null;

    try {
      serverSocket = new ServerSocket( port );

      InetAddress ia = getIPv4Address(getNetworkInterface());
      if (ia != null) {

        String socketAddress = "http://" + ia.toString().substring(1) + ":" + serverSocket.getLocalPort() + "/foo";
        Log.d(TAG, "ServerSocket created at " + socketAddress);

        final Intent i = new Intent(this, MainActivity.class);
        // Needs to be single top so that we only get one instance of Main
        intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

        final NotificationCompat.Builder nb = new NotificationCompat.Builder(this);
        nb.setContentTitle("MediaPlayerServer Running");
        nb.setContentText(socketAddress);
        nb.setWhen(System.currentTimeMillis());
        nb.setContentIntent(contentIntent);
        nb.setOngoing(true);
        nb.setTicker(socketAddress);

        Log.d(TAG, "Setting service to foreground state");
        startForeground(1, nb.build());

        new SocketHandlerThread(serverSocket, socketAddress).start();

      }
    } catch (IOException e) {
      Log.d(TAG, "", e);
    } finally {
    }

    Log.d(TAG, "start <<<");
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy >>>");
    super.onDestroy();
    Log.d(TAG, "onDestroy <<<");
  }

  @Override
  public void onCreate() {
    Log.d(TAG, "onCreate >>>");
    super.onCreate();
    Log.d(TAG, "onCreate <<<");
  }

  private void close(final Socket c) {
    if (c != null) {
      try {
        c.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void close(final ServerSocket c) {
    if (c != null) {
      try {
        c.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void close(final Closeable c) {
    if (c != null) {
      try {
        c.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
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
      if (i.getName().startsWith("wlan") || i.getName().startsWith("eth")) {
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


  private class ConnectionHandlerThread extends Thread {
    private final ServerSocket mServerSocket;
    private final Socket mSocket;

    public ConnectionHandlerThread(final ServerSocket serverSocket, final Socket socket) {
      this.mServerSocket = serverSocket;
      this.mSocket = socket;
    }

    @Override
    public void run() {

      OutputStream out = null;
      BufferedWriter bw = null;

      while (!mSocket.isClosed()) {
        try {
          BufferedReader br = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

          HttpMethod method = null;
          boolean rangeRequest = false;

          int rangeFrom = 0;
          int rangeTo = 2461360;

          while (true) {
            String s = br.readLine();
            if ("".equals(s)) {
              break;
            } else {
              Log.d(TAG, s);
              if (method == null) {
                String[] tokens = s.split(" ");
                if (tokens.length > 0) {
                  method = HttpMethod.getByMethodString(tokens[0]);
                } else {
                  method = HttpMethod.UNDEFINED;
                }
              }

              if (s.startsWith("Range:")) {
                rangeRequest = true;
                String[] fromAndTo = s.split("=")[1].split("-");
                rangeFrom = Integer.valueOf(fromAndTo[0]);
                if (fromAndTo.length > 1) {
                  rangeTo = Integer.valueOf(fromAndTo[1]);
                }
              }

            }
          }

          Bundle b = new Bundle();
          b.putSerializable(Extras.REQUEST_TYPE, method);
          sendBroadcast(Action.EVENT, Category.REQUEST, b);

          if (method == HttpMethod.GET || method == HttpMethod.HEAD) {

            out = new BufferedOutputStream(mSocket.getOutputStream());
            bw = new BufferedWriter(new OutputStreamWriter(out));

            bw.write("HTTP/1.1 " + (rangeRequest?"206 Partial Content":"200 OK") + CRLF);
            bw.write(("Date: " + new SimpleDateFormat("EEE, dd MMM yyyyy HH:mm:ss z").format(new Date())) + CRLF);
            bw.write("Server: My Dicky Server" + CRLF);
            bw.write("Last-Modified: 23 Nov 2011 16:50:22 GMT" + CRLF);
            bw.write("Content-Length: " + (rangeTo - rangeFrom + 1) + CRLF);
            bw.write("Content-Type: audio/mpeg" + CRLF);
            bw.write("Accept-Ranges: bytes" + CRLF);
            if (rangeRequest) {
              bw.write("Content-Range: bytes " + rangeFrom + "-" + rangeTo + "/2461361" + CRLF);
            }

            bw.write(CRLF);
            bw.flush();

            if (method == HttpMethod.GET) {
              Resources resources = getResources();
              int id = resources.getIdentifier(MainService.class.getPackage().getName() + ":raw/test", null, null);
              InputStream rin = resources.openRawResource(id);
              rin.skip(rangeFrom);
              IOUtils.copy(rin, out);
            }
          }

        } catch (IOException e) {
          Log.d(TAG, "", e);
          close(mSocket);
          break;
        } finally {
          close(out);
          close(bw);
        }

      }
      sendBroadcast(Action.EVENT, Category.STOPPED);
    }
  }


  private class SocketHandlerThread extends Thread {
    private final ServerSocket mServerSocket;
    private final String mAddress;

    public SocketHandlerThread(final ServerSocket serverSocket, final String address) {
      this.mServerSocket = serverSocket;
      this.mAddress = address;
    }

    @Override
    public void run() {
      Bundle b = new Bundle();
      b.putString(Extras.SERVER_URL, mAddress);
      sendBroadcast(Action.EVENT, Category.STARTED, b);

      try {
        while (true) {
          Socket socket = mServerSocket.accept();

          b = new Bundle();
          b.putString(Extras.CONNECTION_URL, socket.getInetAddress().toString());
          sendBroadcast(Action.EVENT, Category.CONNECTED, b);

          new ConnectionHandlerThread(mServerSocket, socket).start();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  @Override
  public IBinder onBind(final Intent intent) {
    return null;
  }

}