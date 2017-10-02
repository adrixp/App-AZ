package az.lahermandad.com.azhermandad;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.net.Uri;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class Menu extends AppCompatActivity {

	static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    private static final String TAG = "Menu";

    private final String url = "http://az.tickets.lahermandad.es/api/sell";
    String token = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_menu);

        Bundle extras = getIntent().getExtras();
        token = extras.getString("token");

        Log.d(TAG, "token: " + token);
	}


    public void scanQR(View view) {
		try {
			Intent intent = new Intent(Menu.this, SimpleScannerActivity.class);
			startActivityForResult(intent, 0);
		} catch (ActivityNotFoundException anfe) {
            Toast.makeText(getBaseContext(), getString(R.string.QrLoadFail), Toast.LENGTH_LONG).show();
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.v(TAG, "Llega activty result");
		if (requestCode == 0) {
            Log.v(TAG, "LLega al 0");
			if (resultCode == Activity.RESULT_OK) {
				String qrText = intent.getStringExtra("SCAN_RESULT");

                showDialogResult(Menu.this, true , "QR Result" , qrText, "Ok").show();
                Log.v(TAG, qrText);
			}
		}
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

        final String valHeader = token;

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

}
