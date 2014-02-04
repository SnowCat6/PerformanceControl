package com.brewcrewfoo.performance.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;
import com.brewcrewfoo.performance.util.Prop;
import com.brewcrewfoo.performance.util.PropAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by h0rn3t on 03.02.2014.
 * http://forum.xda-developers.com/member.php?u=4674443
 */
public class IOSetActivity  extends Activity implements Constants, AdapterView.OnItemClickListener, ActivityThemeChangeInterface {
    private boolean mIsLightTheme;
    SharedPreferences mPreferences;
    private final Context context=this;
    Resources res;
    private List<Prop> props = new ArrayList<Prop>();
    private ListView packList;
    private LinearLayout linlaHeaderProgress,nofiles;
    private RelativeLayout tools;
    private PropAdapter adapter;
    private String curio;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        res = getResources();
        setTheme();
        setContentView(R.layout.prop_view);

        Intent i=getIntent();
        curio=i.getStringExtra("curio");

        packList = (ListView) findViewById(R.id.applist);
        packList.setOnItemClickListener(this);
        linlaHeaderProgress = (LinearLayout) findViewById(R.id.linlaHeaderProgress);
        nofiles = (LinearLayout) findViewById(R.id.nofiles);
        tools = (RelativeLayout) findViewById(R.id.tools);

        linlaHeaderProgress.setVisibility(LinearLayout.VISIBLE);
        nofiles.setVisibility(LinearLayout.GONE);
        tools.setVisibility(RelativeLayout.GONE);

        Button applyBtn = (Button) findViewById(R.id.applyBtn);
        Switch setOnBoot = (Switch) findViewById(R.id.applyAtBoot);
        setOnBoot.setChecked(mPreferences.getBoolean(IO_SOB, false));

        applyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                final StringBuilder sb = new StringBuilder();
                final String s=mPreferences.getString(curio.replace(" ","_"),"");

                if(!s.equals("")){
                    String p[]=s.split(";");
                    for(byte i=0;i<2; i++){
                        if (new File(IO_TUNABLE_PATH.replace("mmcblk0","mmcblk"+i)).exists()){
                            for (String aP : p) {
                                if(aP.contains(":")){
                                    final String pn[]=aP.split(":");
                                    sb.append("busybox echo ").append(pn[1]).append(" > ").append(IO_TUNABLE_PATH.replace("mmcblk0","mmcblk"+i)).append("/").append(pn[0]).append(";\n");
                                }
                            }
                        }
                    }
                    final String r= Helpers.shExec(sb, context, true);
                    if((r==null)||!r.equals("nok"))
                        Toast.makeText(context, getString(R.string.applied_ok), Toast.LENGTH_SHORT).show();
                }
            }
        });

        setOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPreferences.edit().putBoolean(IO_SOB, isChecked).apply();
            }
        });

        new GetPropOperation().execute();

    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private class GetPropOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Helpers.get_assetsScript("utils",context,"","");
            new CMDProcessor().sh.runWaitFor("busybox chmod 750 "+getFilesDir()+"/utils" );
            CMDProcessor.CommandResult cr =null;
            cr = new CMDProcessor().su.runWaitFor(getFilesDir()+"/utils -getprop \""+IO_TUNABLE_PATH+"/*\"");
            if(cr.success()){
                load_prop(cr.stdout);
                return "ok";
            }
            else{
                Log.d(TAG, "GovSettings error: " + cr.stderr);
                return "nok";
            }
        }
        @Override
        protected void onPostExecute(String result) {
            if(result.equals("nok")) {
                linlaHeaderProgress.setVisibility(LinearLayout.GONE);
                nofiles.setVisibility(LinearLayout.VISIBLE);
            }
            else{
                linlaHeaderProgress.setVisibility(LinearLayout.GONE);
                if(props.isEmpty()){
                    nofiles.setVisibility(LinearLayout.VISIBLE);
                }
                else{
                    nofiles.setVisibility(LinearLayout.GONE);
                    tools.setVisibility(RelativeLayout.VISIBLE);
                    adapter = new PropAdapter(IOSetActivity.this, R.layout.prop_item, props);
                    packList.setAdapter(adapter);
                }
            }
        }
        @Override
        protected void onPreExecute() {}
        @Override
        protected void onProgressUpdate(Void... values) {}
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long row) {
        final Prop p = adapter.getItem(position);
        editPropDialog(p);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.vm_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.reset_vm) {
            if(!props.isEmpty()){
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.mt_reset))
                        .setMessage(getString(R.string.reset_msg))
                        .setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                        .setPositiveButton(getString(R.string.yes),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mPreferences.edit().remove(curio.replace(" ","_")).apply();
                                    }
                                }).create().show();
            }
        }
        return true;
    }
    @Override
    public boolean isThemeChanged() {
        final boolean is_light_theme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        return is_light_theme != mIsLightTheme;
    }

    @Override
    public void setTheme() {
        final boolean is_light_theme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        mIsLightTheme = is_light_theme;
        setTheme(is_light_theme ? R.style.Theme_Light : R.style.Theme_Dark);
    }
    @Override
    public void onResume() {
        super.onResume();
    }

    private void editPropDialog(Prop p) {
        final Prop pp=p;
        LayoutInflater factory = LayoutInflater.from(this);
        final View editDialog = factory.inflate(R.layout.prop_edit_dialog, null);
        final EditText tv = (EditText) editDialog.findViewById(R.id.vprop);
        final TextView tn = (TextView) editDialog.findViewById(R.id.nprop);
        tv.setText(pp.getVal());
        tn.setText(pp.getName());

        new AlertDialog.Builder(this)
                .setTitle(curio)
                .setView(editDialog)
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if ((tv.getText().toString() != null) && (tv.getText().toString().length() > 0)) {
                            pp.setVal(tv.getText().toString().trim());
                            set_pref(tn.getText().toString().trim(), tv.getText().toString().trim());
                        }
                        adapter.notifyDataSetChanged();
                    }
                }).create().show();
    }

    public void set_pref(String n, String v){
        final String s=mPreferences.getString(curio.replace(" ","_"),"");
        final StringBuilder sb = new StringBuilder();
        if(!s.equals("")){
            String p[]=s.split(";");
            for (String aP : p) {
                if(aP!=null && aP.contains(":")){
                    final String pn[]=aP.split(":");
                    if(!pn[0].equals(n)) sb.append(pn[0]).append(':').append(pn[1]).append(';');
                }
            }
        }
        sb.append(n).append(':').append(v).append(';');
        mPreferences.edit().putString(curio.replace(" ","_"), sb.toString()).commit();
    }
    public void load_prop(String s){
        props.clear();
        if(s==null) return;
        final String p[]=s.split("\n");
        for (String aP : p) {
            try{
                if(aP!=null && aP.contains("::")){
                    String pn=aP.split("::")[0];
                    pn=pn.substring(pn.lastIndexOf("/") + 1, pn.length()).trim();
                    props.add(new Prop(pn,aP.split("::")[1].trim()));
                }
            }
            catch (Exception e){
            }
        }
    }
}