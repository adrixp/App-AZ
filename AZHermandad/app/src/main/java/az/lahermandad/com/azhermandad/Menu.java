package az.lahermandad.com.azhermandad;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.transition.Explode;
import android.transition.TransitionInflater;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.net.Uri;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

public class Menu extends AppCompatActivity {

	static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    private static final String TAG = "Menu";

    boolean canStartHandler = true;
    private int mInterval = 500; // 0.5 seconds by default
    private Handler mHandler;
    String strTo64 = "empty";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_menu);
	}


    @Override
    public void onResume() {
        super.onResume();

        if(canStartHandler){
            strTo64 = readLogin();
            mHandler = new Handler();
            startRepeatingTask();
        }
        System.out.println("onResume");
    }

    @Override
    public void onPause() {
        System.out.println("onPause");
        if(mHandler != null){
            stopRepeatingTask();
        }
        super.onPause();
    }

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    static String readLogin(){
        String path = Environment.getExternalStorageDirectory().getPath() + "/AZ_LaHermandad";
        File file = new File(path, "tmp");
        //Read text from file
        String line = "empty";

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            line = br.readLine();
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
            line = "empty";
        }

        if(line != null ){
            if(!line.equals("empty")) {
                byte[] data = Base64.decode(line, Base64.DEFAULT);
                String text = new String(data);
                String[] parts = text.split(":");

                line = parts[0] + ":" + parts[1];
            }
        }
        System.out.println("ReadFile: Login: " + line);
        return line;
    }

    public void scanQR(View view) {
		try {
			Intent intent = new Intent(ACTION_SCAN);
			intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
			startActivityForResult(intent, 0);
		} catch (ActivityNotFoundException anfe) {
			showDialog(Menu.this, "No Scanner Found", "Download a scanner code activity?", "Yes", "No").show();
		}
	}

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                if(!strTo64.equals("empty")){
                    makeRequest();
                }
            } finally {
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    public void makeRequest(){
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://test-az-tickets.herokuapp.com/api/sell";
        byte[] b64 = Base64.encode(strTo64.getBytes(),Base64.DEFAULT);
        final String valHeader = "Basic " + new String(b64);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        //Toast.makeText(getBaseContext(), "GET: Response is: "+ response.substring(0,500), Toast.LENGTH_LONG).show();
                        System.out.println("Response: MakeRequest: " + response);
                        try {
                            String path = Environment.getExternalStorageDirectory().getPath() + "/AZ_LaHermandad";
                            File settings = new File(path, "tmp2");
                            FileOutputStream fos2 = new FileOutputStream(settings);

                            fos2.write(response.getBytes());
                            fos2.close();

                        } catch (java.io.IOException e) {
                            Log.e(TAG, "Exception in writeFile", e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                try{
                    System.out.print("Error: " + error.networkResponse.statusCode);
                }catch (Exception e){
                    System.out.print("Error: " + error.getMessage());
                }
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> mHeaders = new ArrayMap<String, String>();
                mHeaders.put("Authorization", valHeader);
                mHeaders.put("Content-Type", "text/plain; charset=utf-8");
                return mHeaders;
            }
        };
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		System.out.println("onActivityResult");
        System.out.println("canStartHandler: " + canStartHandler);
        canStartHandler = false;

        strTo64 = readLogin();
        mHandler = new Handler();
        startRepeatingTask();

		if (requestCode == 0) {
			if (resultCode == RESULT_OK) {
				String json = intent.getStringExtra("SCAN_RESULT");
				//String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

                System.out.println("QR: EntradaLeida: " + json);
                try {
                    Gson gson = new Gson();
                    Type stringStringMap = new TypeToken<Map<String, String>>(){}.getType();
                    Map<String, String> myMapEntrada = gson.fromJson(json, stringStringMap);
                    int stateEntrada = 5;

                    if (myMapEntrada.get("user") == null || myMapEntrada.get("name") == null || myMapEntrada.get("mail") == null){
                        stateEntrada = 3;
                    }

                    String entradaUser = myMapEntrada.get("user");
                    String entradaName = myMapEntrada.get("name");
                    String entradaMail = myMapEntrada.get("mail");
                    String entradaSc = myMapEntrada.get("securityCode");
                    String EntradaDelete = myMapEntrada.get("delete");
                    String numeroEntrada = "-";
                    myMapEntrada.clear();

                    if (EntradaDelete.equals("true")) {
                        stateEntrada = 0;
                    }else {

                        String listJson = readFileRequest();
                        listJson = listJson.substring(1, listJson.length() - 2); //sin los []
                        System.out.println("listJson Sin alante y sin atras" + listJson);
                        String partsJson[] = listJson.split("[}],");
                        Map<String, String> myMapLista;
                        String jsonParted = "";

                        boolean proccesed = false;

                        for (int i = 0; i < partsJson.length; i++) {

                            if (i < partsJson.length - 1) {
                                myMapLista = gson.fromJson(partsJson[i] + "}", stringStringMap);
                            }else{
                                myMapLista = gson.fromJson(partsJson[i], stringStringMap);
                            }

                            if (myMapLista.get("user").equals(entradaUser) && myMapLista.get("name").equals(entradaName) &&
                                    myMapLista.get("mail").equals(entradaMail) && myMapLista.get("securityCode").equals(entradaSc) &&
                                    myMapLista.get("use").equals("true")) {
                                if(!proccesed){
                                    stateEntrada = 2;
                                    proccesed = true;
                                    int number = i+1;
                                    numeroEntrada = "" + number;
                                }
                            } else if (myMapLista.get("user").equals(entradaUser) && myMapLista.get("name").equals(entradaName) &&
                                    myMapLista.get("mail").equals(entradaMail) && myMapLista.get("securityCode").equals(entradaSc) &&
                                    myMapLista.get("use").equals("false")) {
                                if(!proccesed){
                                    System.out.println("al partir : " + json);
                                    String laputa [] = json.split("\"use\":false");
                                    json = laputa[0] + "\"use\":true" + laputa[1];
                                    int number = i+1;
                                    numeroEntrada = "" + number;
                                    stateEntrada = 1;
                                    proccesed = true;
                                }
                            }

                            myMapLista.clear();

                        }
                        if(!proccesed){
                            stateEntrada = 4;
                        }

                        System.out.println("QR: UpdateLista: " + json);
                        if(proccesed &&  stateEntrada == 1){
                            sendPost(json);
                        }

                    }

                    showStateEntrada(entradaUser, entradaName, entradaMail, stateEntrada, numeroEntrada);

                }catch (Exception e){
                    showStateEntrada("", "", "", 3 , "-");
                    e.printStackTrace();
                }
			}
		}
	}

    public static String readFileRequest(){
        String path = Environment.getExternalStorageDirectory().getPath() + "/AZ_LaHermandad";
        File file = new File(path, "tmp2");
        //Read text from file
        String line;
        String text = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            while ((line = br.readLine()) != null) {
                text = line + "\n";
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
            text = "";
        }
        System.out.println("ReadFileREquest: texto Leido: " + text);
        return text;
    }


    public void showStateEntrada (String entradaUser, String entradaName, String entradaMail, int stateEntrada, String numEntrada){
        String entradaRestult;
        switch (stateEntrada){
            case 0:
                entradaRestult = "Entrada Borrada";
                break;
            case 1:
                entradaRestult = "Entrada OK";
                break;
            case 2:
                entradaRestult = "Entrada Repetida";
                break;
            case 3:
                entradaRestult = "Entrada Invalida";
                break;
            case 4:
                entradaRestult = "Entrada No Encontrada";
                break;
            default:
                entradaRestult = "Error Desconocido";
                break;
        }
        String body =  "*Vendedor:\n" + entradaUser + "\n*Participante:\n" + entradaName
                + "\n*Mail:\n" + entradaMail + "\n*Número: " +numEntrada;

        if(stateEntrada == 1){
            showDialogResult(Menu.this, true , entradaRestult , body, "Ok").show();
        }else{
            showDialogResult(Menu.this, false ,entradaRestult, body, "Ok").show();
        }

    }
    public void sendPost(String strPost) {
        RequestQueue queue = Volley.newRequestQueue(this);
        final String url = "http://test-az-tickets.herokuapp.com/api/sell";
        byte[] b64 = Base64.encode(strTo64.getBytes(),Base64.DEFAULT);
        final String valHeader = "Basic " + new String(b64);
        System.out.println("headers post: " + valHeader);
        System.out.println("headers body: " + strPost);

        try {
            JSONObject jsonobj = new JSONObject(strPost);
            JsonObjectRequest jsonArrayRequest = new JsonObjectRequest(Request.Method.POST, url, jsonobj, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    System.out.print("Response OK post: " + response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    System.out.print("Response FAIL post: " + error);
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> mHeaders = new ArrayMap<String, String>();
                    mHeaders.put("Content-Type", "application/json");
                    mHeaders.put("Authorization", valHeader);
                    return mHeaders;
                }
            };
            queue.add(jsonArrayRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static AlertDialog showDialogResult(final Activity act, boolean isOk, CharSequence title, CharSequence message, CharSequence buttonYes) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);
        if(isOk){
            downloadDialog.setIcon(R.mipmap.ic_gtick);
        }else{
            downloadDialog.setIcon(R.mipmap.ic_redcross);
        }
        downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        return downloadDialog.show();
    }


    private static AlertDialog showDialog(final Activity act, CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);
        downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    act.startActivity(intent);
                } catch (ActivityNotFoundException anfe) {
                    anfe.printStackTrace();
                }
            }
        });
        downloadDialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return downloadDialog.show();
    }

}
