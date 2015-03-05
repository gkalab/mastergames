package com.kalab.mastergames;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

    public static final String PGN_MASTER_ID = "com.kalab.pgnviewer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container,
                    new PlaceholderFragment()).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getPackageManager().getLaunchIntentForPackage(PGN_MASTER_ID);
        updateButtonText(intent);
    }

    private void updateButtonText(Intent intent) {
        Button button = (Button) findViewById(R.id.startButton);
        if (button != null) {
            if (intent != null) {
                button.setText(getText(R.string.start));
            } else {
                button.setText(getText(R.string.install));
            }
        }
    }

    public void onButtonClick(View view) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(PGN_MASTER_ID);
        if (intent == null) {
            Intent appIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + PGN_MASTER_ID));
            startActivity(appIntent);
        } else {
            startActivity(intent);
            finish();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_license) {
            SpannableString message = new SpannableString(getString(R.string.license));
            Linkify.addLinks(message, Linkify.ALL);
            final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setPositiveButton(android.R.string.ok, null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setMessage(message).create();
            dialog.show();
            ((TextView) dialog.findViewById(android.R.id.message))
                    .setMovementMethod(LinkMovementMethod.getInstance());

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container,
                    false);
            return rootView;
        }
    }
}
