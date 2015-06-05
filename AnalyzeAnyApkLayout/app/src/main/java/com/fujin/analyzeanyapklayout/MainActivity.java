package com.fujin.analyzeanyapklayout;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dalvik.system.PathClassLoader;


public class MainActivity extends ActionBarActivity {

    private EditText et;
    private Button load;

    private TypedValue mTmpValue;

    private ExecutorService executor;

    private ProgressDialog dialog;

    private PopupWindow popupWindow;

    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            dialog.dismiss();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dialog=new ProgressDialog(this);
        dialog.setTitle("正在解析");

        et= (EditText) findViewById(R.id.et);

        load= (Button) findViewById(R.id.load);


        et.setText("com.tencent.mobileqq");

        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();

                final String packageName = et.getText().toString().trim();

                if (!TextUtils.isEmpty(packageName)) {

                    new Thread() {
                        public void run() {
                            try {
                                executor = Executors.newFixedThreadPool(6);

                                Context context = createPackageContext(packageName, CONTEXT_IGNORE_SECURITY | CONTEXT_INCLUDE_CODE);
                                PathClassLoader loader = new PathClassLoader(context.getPackageResourcePath(), ClassLoader.getSystemClassLoader());
                                Class clazz = Class.forName(packageName + ".R$layout", true, loader);

                                Field[] fields = clazz.getDeclaredFields();

                                for (final Field field : fields) {
                                    field.setAccessible(true);


                                    if (!field.getName().startsWith("abc")) {


                                        int id = field.getInt(R.layout.class);
                                        XmlResourceParser parser = context.getResources().getLayout(id);

                                        Log.i("AAAA", parser.getClass().getSimpleName());
                                        final AttributeSet attrs = Xml.asAttributeSet(parser);

                                        parser.getText();


                                        int type = parser.getEventType();

                                        final StringBuilder builder = new StringBuilder();

                                        while (type != XmlResourceParser.END_DOCUMENT) {
                                            switch (type) {
                                                case XmlResourceParser.START_DOCUMENT:

                                                    builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");

                                                    break;
                                                case XmlResourceParser.START_TAG:

                                                    builder.append("<" + parser.getName() + "\n");
                                                    int attrCount = parser.getAttributeCount();

                                                    for (int i = 0; i < attrCount; i++) {
                                                        String name = parser.getAttributeName(i);
                                                        String value = parser.getAttributeValue(i);


                                                        builder.append("    " + name + "=" + "\"" + value + "\"\n");

                                                        if (i == attrCount - 1) {
                                                            builder.append(">\n");
                                                        }

                                                    }


                                                    break;

                                                case XmlPullParser.END_TAG:
                                                    builder.append("</" + parser.getName() + ">\n");
                                                    break;

                                            }


                                            type = parser.next();

                                        }

                                        executor.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                writeToFile(field.getName(), builder.toString());
                                            }
                                        });


//                                TypedValue value = mTmpValue;
//                                if (value == null) {
//                                    mTmpValue = value = new TypedValue();
//                                }
//                                getValue(context,id, value, true);

//                                Log.i("AAAAA",value.string.toString());


                                    }


                                }


                                while (!executor.isTerminated()) {
                                    SystemClock.sleep(1000);

                                }



                                dialog.dismiss();

                                executor.shutdown();


                            } catch (PackageManager.NameNotFoundException e) {
                                Toast.makeText(MainActivity.this, "包名称不存在", Toast.LENGTH_SHORT).show();
                            } catch (ClassNotFoundException e) {
                                Toast.makeText(MainActivity.this, "资源文件不存在", Toast.LENGTH_SHORT).show();

                            } catch (IllegalAccessException e) {
                                Toast.makeText(MainActivity.this, "访问全县不足", Toast.LENGTH_SHORT).show();
                            } catch (XmlPullParserException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }.start();


                } else {
                    Toast.makeText(MainActivity.this, "包名不能为null", Toast.LENGTH_SHORT).show();
                }

            }
        });



       // logPackageName();


    }


    public void logPackageName()
    {
        PackageManager pm=getPackageManager();
        List<PackageInfo> infos= pm.getInstalledPackages(0);

        for (PackageInfo info:infos)
        {
            Log.i("AAAA",info.packageName);
        }


    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void getValue(Context context,int id, TypedValue outValue, boolean resolveRefs)
            throws Resources.NotFoundException {
        boolean found =getResourceValue(context.getAssets(), id, 0, outValue, resolveRefs);
        if (found) {
            return;
        }
        throw new Resources.NotFoundException("Resource ID #0x"
                + Integer.toHexString(id));
    }

   boolean  getResourceValue(AssetManager manager, int ident,
                     int density,
                     TypedValue outValue,
                     boolean resolveRefs)
    {

        try {
            Method method=AssetManager.class.getDeclaredMethod("loadResourceValue",int.class,short.class,TypedValue.class,boolean.class);
            method.setAccessible(true);
            int block= (int) method.invoke(manager,ident, (short) density, outValue, resolveRefs);

            if (block >= 0) {
                if (outValue.type != TypedValue.TYPE_STRING) {
                    return true;
                }

                Field field=AssetManager.class.getDeclaredField("mStringBlocks");

               Class clazz= Class.forName("android.content.res.StringBlock");
                Object obj= field.get(manager);
                Method method1=clazz.getDeclaredMethod("get", int.class);
                method1.setAccessible(true);
                outValue.string = (CharSequence) method1.invoke(obj,outValue.data);

                return true;
            }
            return false;
        } catch (Exception e) {
            return  false;
        }

    }


    private void writeToFile(String fileName,String content)
    {
        byte data[]=content.getBytes();

        FileChannel channel=getWriteChannel(fileName);

        ReadableByteChannel src = Channels.newChannel(new ByteArrayInputStream(data));


        try {
            channel.transferFrom(src,0,data.length);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try{
                if (channel!=null)
                    channel.close();
                if (src!=null)
                    src.close();
            }catch (Exception e)
            {
                e.printStackTrace();
            }

        }
    }

    private FileChannel getWriteChannel(String fileName)
    {
        File file=new File(Environment.getExternalStorageDirectory().getAbsoluteFile(),fileName+".xml");

        if (file.exists())
            file.delete();
        try {
            FileOutputStream fos=new FileOutputStream(file);
            return fos.getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

    }


    private PopupWindow showPopWindow(Context context)
    {
        PopupWindow popupWindow=new PopupWindow(context);
        TextView tv=new TextView(context);
        tv.setText("正在加载 ");
        popupWindow.setContentView(tv);

        popupWindow.showAsDropDown(tv);
        return  popupWindow;
    }
}
