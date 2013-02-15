package com.we7.player.mediaplayerserver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.we7.player.mediaplayerserver.MainService.Action;
import com.we7.player.mediaplayerserver.MainService.Category;
import com.we7.player.mediaplayerserver.MainService.Extras;

public class MainActivity extends Activity implements OnClickListener {

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    IntentFilter f = new IntentFilter();
    f.addAction(Action.EVENT);
    f.addCategory(Category.STARTED);
    f.addCategory(Category.STOPPED);
    LocalBroadcastManager.getInstance(this).registerReceiver(eventReceiver, f);
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem m) {
    boolean r = false;
    switch (m.getItemId()) {
      case R.id.menu_exit:
        finish();
        r = true;
        break;
    }
    return r;
  }

  @Override
  public void onClick(final View v) {
    switch (v.getId()) {
      case R.id.start:
        sendToService(v.getContext(), Action.CONTROL, Category.START);
        break;
      default:
    }
  }

  private void sendToService(final Context context, final String action, final String category) {
    final Intent i = new Intent(context, MainService.class);
    i.setAction(action);
    i.addCategory(category);
    context.startService(i);
  }

  private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(final Context context, final Intent intent) {
      if (Action.EVENT.equals(intent.getAction())) {
        event(intent);
      }
    }

    private void event(final Intent intent) {
      for (String category : intent.getCategories()) {
        if (Category.STARTED.equals(category)) {
          started(intent);
        } else if (Category.STOPPED.equals(category)) {
          stopped(intent);
        }
      }
    }

    private void stopped(final Intent intent) {
      findViewById(R.id.start).setEnabled(true);
    }

    private void started(final Intent intent) {
      findViewById(R.id.start).setEnabled(false);

      TextView tv = (TextView) findViewById(R.id.text);
      tv.setText(intent.getExtras().getString(Extras.SERVER_URL, ""));
    }

  };

}
