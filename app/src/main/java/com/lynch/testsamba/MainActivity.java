package com.lynch.testsamba;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;

import jcifs.smb.SmbFile;
import com.lynch.testsamba.util.DialogUtil;
import com.lynch.testsamba.util.IntentUtils;
import com.lynch.testsamba.util.UriUtil;
import qpsamba.IConfig;
import qpsamba.IConfig.OnConfigListener;
import qpsamba.SambaUtil;
import qpsamba.httpd.NanoStreamer;


public class MainActivity extends SambaActivity implements Toolbar.OnMenuItemClickListener {// TODO: 2016/8/2 change 2 actioBar
    //    private final static String TAG = "MainActivity";
    //public final static String SMB_VIDEO_URL_CIF46000 = "smb://;ram:1234@RAM-ELEM/samba/videos/cif-46000.mp4";
    private Spinner fileSpinner;
    private Spinner wgSpinner;
    private TextView tvResult;
    private TextView tvSelectedFile;
    // private TextView tvSelectedWG;
    private EditText editText;

    protected ArrayAdapter<String> fileAdapter;
    protected ArrayAdapter<String> wgAdapter;

    protected boolean isAutoSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, StreamService.class));
        setContentView(R.layout.activity_samba);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_samba);
        toolbar.setOnMenuItemClickListener(this);
        //setActionBar(toolbar);

        tvResult = (TextView) findViewById(R.id.tv_result);
        tvSelectedFile = (TextView) findViewById(R.id.tv_selected_file);
        //   tvSelectedWG = (TextView) findViewById(R.id.tv_selected_workgroup);
        listRoot();

        initSpinner();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, StreamService.class));
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_samba, menu);
        return true;
    }*/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode != REQUEST_CODE_CHOOSE_IMAGE) {
            return;
        }
        try {
            Uri selectedImage = data.getData();
            String path = UriUtil.getImagePath(this, selectedImage);
            upload(path);// TODO: 2016/8/4  
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         int id = item.getItemId();
 //        Log.e("curSmbAddr", curRemoteFile);// TODO: 2016/8/2
         if (id == R.id.action_change_host) {
             showAccountDialog();
         } else if (id == R.id.action_smb_home) {
             listRoot();
         } else if (id == R.id.action_upload) {
             IntentUtils.pickupImages(this, REQUEST_CODE_CHOOSE_IMAGE);
         } else if (id == R.id.action_upload_video) {
             IntentUtils.pickupVideo(this, REQUEST_CODE_CHOOSE_IMAGE);
         } else if (id == R.id.action_play_video) {
             playVideo(curRemoteFile, false);
         } else if (id == R.id.action_play_video_3rd) {
             playVideo(curRemoteFile, true);
         } else if (id == R.id.action_download) {
             download(genLocalPath(), curRemoteFile);
         } else if (id == R.id.action_clear) {
             tvResult.setText("CLEARED");
         } else if (id == R.id.action_create_folder) {
             showCreateDialog();
         } else if (id == R.id.action_delete_file) {
             showDeleteDialog(curRemoteFile);
         } else if (id == R.id.action_delete_folder) {
             showDeleteDialog(curRemoteFolder);
         } else if (id == R.id.action_list_workgroup) {
             listWorkGroup();// TODO: 2016/8/2 list the most files
         }
         return super.onOptionsItemSelected(item);
     }
 */
    private void showCreateDialog() {
        editText = DialogUtil.showInput(this, "Please input folder name", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which != DialogInterface.BUTTON_POSITIVE) {
                    return;
                }
                createFolder(editText.getText().toString());
            }
        });
    }

    private void showCreatefDialog() {
        editText = DialogUtil.showInput(this, "Please input file name", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which != DialogInterface.BUTTON_POSITIVE) {
                    return;
                }
                createfile(editText.getText().toString());
            }
        });
    }

    private void showDeleteDialog(final String path) {
        if (TextUtils.isEmpty(path)) {
            Toast.makeText(this, "INVALID FILE/FOLDER", Toast.LENGTH_LONG).show();
            return;
        }
        DialogUtil.showConfirmDialog(//
                this,//
                new StringBuilder("Confirm to Delete \n\"").append(path).append("\"?").toString(), //
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which != DialogInterface.BUTTON_POSITIVE) {
                            return;
                        }
                        delele(path);
                    }
                });
    }

    private void showAccountDialog() {
        DialogUtil.showInputAccoutDialog(//
                this,//
                new OnConfigListener() {
                    @Override
                    public void onConfig(IConfig config, Object obj) {
                        Log.d(TAG, "" + config);
                        mConfig = config;
                        listRoot();
                        //updateWorkgroupUI();
                    }
                }
        );
    }

    /* * handler Spinner****/
    private void initSpinner() {
        //------------

        //listWorkGroup();

       /* wgSpinner = (Spinner) findViewById(R.id.sp_workgroup);
        wgAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
        wgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        wgSpinner.setAdapter(wgAdapter);
        wgSpinner.setOnItemSelectedListener(//
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String host = wgAdapter.getItem(position);
                        Log.d(TAG, "wgSpinner onItemSelected  " + host);
                        if (TextUtils.isEmpty(host)) {
                            return;
                        }
                        mConfig.updateHost(host);
                        Log.d(TAG, "wgSpinner onItemSelected  " + mConfig);
                        listRoot();
                        updateWorkgroupUI();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        Log.d(TAG, "wgSpinner   onNothingSelected  curRemoteFolder=" + curRemoteFolder + "    isAutoSelected=" + isAutoSelected);
                    }
                }
        );*/

        //------------
        fileSpinner = (Spinner) findViewById(R.id.sp_file);
        fileAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
        fileAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fileSpinner.setAdapter(fileAdapter);
        fileSpinner.setOnItemSelectedListener(//
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String name = fileAdapter.getItem(position);
                        onFileSelected(name);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Log.d(TAG, "fileAdapter onNothingSelected  curRemoteFolder=" + curRemoteFolder + "    isAutoSelected=" + isAutoSelected);
                    }
                }
        );
//        curRemoteFolder = SambaUtil.wrapSmbFullURL(mConfig, "/");
//        loadToSpinner(curRemoteFolder);
    }

    private void onFileSelected(String name) {// TODO: 2016/8/2 open the files' operations 
        Log.d(TAG, "onFileSelected  curRemoteFolder=" + curRemoteFolder + " name=\"" + name + "\"    isAutoSelected=" + isAutoSelected);
        if (isAutoSelected) {
            isAutoSelected = false;
            return;
        }
        if (name.equals(REMOTE_PARENT)) {
            if (curRemoteFolder != null) {
            }
            return;
        }
        SmbFile file = REMOTE_PATHS.get(name);
        tvSelectedFile.setText(file.getName());
        if (name.startsWith(REMOTE_FOLDER_PREFIX)) {
            curRemoteFolder = file.getPath();
            loadToSpinner(curRemoteFolder);
        } else if (name.startsWith(REMOTE_FILE_PREFIX)) {
            curRemoteFile = file.getPath();
            curRemoteFolder = file.getParent();
        }
        updateSelectedUI();
    }


    private void loadToSpinner(final String path) {
        Log.d(TAG, "loadToSpinner    " + path);
        new Thread(new Runnable() {
            @Override
            public void run() {
                listAndPrepare(path); // TODO: 2016/8/1
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fileAdapter.clear();
                        fileAdapter.addAll(REMOTE_PATHS.keySet());
                        fileAdapter.notifyDataSetChanged();
                        isAutoSelected = true;
                        fileSpinner.setSelection(0);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void listRoot() {
        super.listRoot();
        loadToSpinner(curRemoteFolder);
    }

    private final void playVideo(String path, boolean is3RD) {
        if (TextUtils.isEmpty(path)) {
            // path = SMB_VIDEO_URL_CIF46000;
            path = curRemoteFile;

        }
        String mime = SambaUtil.getVideoMimeType(path) + "";
       /* if (!String.valueOf(mime).toLowerCase().startsWith("video")) {
            Toast.makeText(this, "NOT a video file  " + mime, Toast.LENGTH_SHORT).show();
            return;
        }*/
        path = SambaUtil.wrapStreamSmbURL(path, NanoStreamer.INSTANCE().getIp(), NanoStreamer.INSTANCE().getPort());
        if (is3RD) {
            IntentUtils.openVideo(this, path);
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(VideoActivity.ACTION_KEY_URL, path);
        intent.setClass(this, VideoActivity.class);
        startActivity(intent);
    }


    /*private void updateWorkgroupUI() {
        tvSelectedWG.setText("Current Host:" + mConfig.host);
    }*/

    private void updateSelectedUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvSelectedFile.setText("FOLDER:\n");
                tvSelectedFile.append("     ");
                tvSelectedFile.append(String.valueOf(curRemoteFolder));
                tvSelectedFile.append("\n");

                tvSelectedFile.append("FILE:\n");
                tvSelectedFile.append("     ");
                tvSelectedFile.append(curRemoteFile == null ? "NONE FILE SELECTED!" : curRemoteFile);
            }
        });
    }

    private final String genLocalPath() {
        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + LOCAL_FOLDER_PATH);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String name = SambaUtil.getFileName(curRemoteFile);
        if (name == null) {
            name = String.valueOf(System.currentTimeMillis());
        }
        File f = new File(new StringBuilder(folder.getAbsolutePath()).append("/").append(name).toString());
        return f.getAbsolutePath();
    }

    @Override
    protected void updateResult(final String action, final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String ACTION_STR = String.valueOf(action).toUpperCase();
                int MAX_LENGTH = 35;
                int length = ACTION_STR.length();
                final String DIVIDER = "=";
                while (length <= MAX_LENGTH) {
                    ACTION_STR = length % 2 == 0 ? ACTION_STR + DIVIDER : DIVIDER + ACTION_STR;
                    length++;
                }
                StringBuilder builder = new StringBuilder("\n");
                builder.append(ACTION_STR);
                builder.append("\n");
                builder.append(msg);
                tvResult.append(builder);
                Log.d(TAG, "updateResult    " + builder);
            }
        });
    }

    @Override
    protected void onRemoteFolderChange(String path, boolean result) {
        loadToSpinner(path);
    }

    @Override
    protected void onListWorkgroup(String[] paths) {
        if (paths == null) {
            return;
        }
        wgAdapter.clear();
        wgAdapter.addAll(paths);
        wgAdapter.notifyDataSetChanged();
        wgSpinner.setSelection(0);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
//        Log.e("curSmbAddr", curRemoteFile);// TODO: 2016/8/2
        if (id == R.id.action_change_host) {
            showAccountDialog();
        } else if (id == R.id.action_smb_home) {
            listRoot();
        } else if (id == R.id.action_upload) {
            IntentUtils.pickupImages(this, REQUEST_CODE_CHOOSE_IMAGE);
        } else if (id == R.id.action_upload_video) {
            IntentUtils.pickupVideo(this, REQUEST_CODE_CHOOSE_IMAGE);
        } else if (id == R.id.action_play_video) {
            playVideo(curRemoteFile, false);
        } else if (id == R.id.action_play_video_3rd) {
            playVideo(curRemoteFile, true);
        } else if (id == R.id.action_download) {
            download(genLocalPath(), curRemoteFile);
        } else if (id == R.id.action_clear) {
            tvResult.setText("CLEARED");
        } else if (id == R.id.action_create_folder) {
            showCreateDialog();
        } else if (id == R.id.action_delete_file) {
            showDeleteDialog(curRemoteFile);
        } else if (id == R.id.action_delete_folder) {
            showDeleteDialog(curRemoteFolder);
        } else if (id == R.id.createFile) {
            showCreatefDialog();
            //listWorkGroup();// TODO: 2016/8/2 list the most files
        } else if (id == R.id.open) {
            openFile(new File(curRemoteFile));
        }
        return super.onOptionsItemSelected(item);
    }

    public void openFile(final File f) {

        MaterialDialog.Builder a = new MaterialDialog.Builder(this);
        a.title("open as");
        String[] items = {"text", "image", "video", "audio", "other"};
        final Uri uri = Uri.fromFile(f);
        final Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        a.items(items).itemsCallback(new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                switch (i) {   //i.setDataAndType(uri, MimeTypes.getMimeType(new File(smbFile.getPath())));
                    case 0:
                        intent.setDataAndType(uri, "text/*");
                        break;
                    case 1:
                        intent.setDataAndType(uri, "image/*");
                        break;
                    case 2:
                        intent.setDataAndType(uri, "video/*");
                        break;
                    case 3:
                        intent.setDataAndType(uri, "audio/*");
                        break;
                    /*case 5:
                        intent = new Intent(c, DbViewer.class);
                        intent.putExtra("path", f.getPath());
                        break;*/
                    case 4:
                        intent.setDataAndType(uri, "*/*");
                        break;
                }
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        try {
            a.build().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
