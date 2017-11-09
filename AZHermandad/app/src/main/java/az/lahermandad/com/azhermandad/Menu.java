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
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
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

    //private final String url = "https://test-sell-ticket.herokuapp.com/ticket/";
    private final String url = "https://sell-ticket.herokuapp.com/ticket/";
    String token = "";
    boolean canReserve = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_menu);

        Bundle extras = getIntent().getExtras();
        token = extras.getString("token");
        canReserve = extras.getBoolean("canReserve");

        Log.d(TAG, "token: " + token);
        Log.d(TAG, "canReserve: " + canReserve);
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
                String reserved = "";
                String id = "";
                String email = "";
                String name = "";
                String dni = "";
                String date = "";
                boolean inmortal = false;
                Log.v(TAG, qrText);

                try {
                    JSONObject jsonObj = new JSONObject(qrText);
                    reserved = jsonObj.getString("reserved");
                    id = jsonObj.getString("_id");
                    email = jsonObj.getString("email");
                    name = jsonObj.getString("name");
                    dni = jsonObj.getString("dni");
                    date = jsonObj.getString("dateOfPursache");

                    inmortal = jsonObj.getBoolean("immortal");

                    if(canReserve = false && reserved.equals("true")){
                        showDialogResult(Menu.this, false , "QR Result" , "Es una entrada reservada.\nNo tienes permisos para validarla\nDirige al participante a RESERVAS", "Ok").show();
                    }else if(canReserve = true && reserved.equals("true")){
                        sendPatch(qrText,id,email,dni,name,date,true,inmortal);
                    }else{
                        sendPatch(qrText,id,email,dni,name,date,false,inmortal);
                    }

                }catch (JSONException e){
                    e.printStackTrace();
                    showDialogResult(Menu.this, false , "Invalid QR" , "Código QR Inválido", "Ok").show();
                }catch (Exception e){
                    e.printStackTrace();
                    showDialogResult(Menu.this, false , "QR Result" , "Problema desconocido\nVer log", "Ok").show();
                }
            }
		}
	}


    public void sendPatch(final String strPost, final String id, final String email, final String dni, final String name, final String date, final boolean isReserved, boolean isInmortal) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String nofInmo = "";
        if(isInmortal){
            nofInmo = "Es Inmortal\n";
        }

        final String isInmortals = nofInmo;

        try {
            JSONObject jsonObj = new JSONObject(strPost);
            JsonObjectRequest jsonArrayRequest = new JsonObjectRequest(Request.Method.PATCH, url + id + "/used", jsonObj, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.v(TAG, "Response OK PATCH: " + response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.v(TAG, "Response FAIL PATCH: " + error);
                    NetworkResponse networkResponse = error.networkResponse;
                    if(networkResponse != null){
                        Log.v(TAG, "ERROR CODEPATCH: " + networkResponse.statusCode);

                        switch (networkResponse.statusCode){
                            case 401:
                                finish();
                                break;
                            case 403:
                                showDialogResult(Menu.this, false , "QR Result" , "Es una entrada reservada.\nNo tienes permisos para validarla\nDirige al participante a RESERVAS", "Ok").show();
                                break;
                            case 409:
                                showDialogResult(Menu.this, false , "Entrada Repetida" , "Esta entrada ya ha sido validada \nPor favor que el participante vaya a RESERVAS\nDorsal: "
                                         + id + "\n" + "DNI: " + dni + "\nNombre: " + name
                                        , "Ok").show();
                                break;
                            default:
                                showDialogResult(Menu.this, false , "Error " + networkResponse.statusCode, "Error interno del servidor\n" + strPost, "Ok").show();
                                break;
                        }
                    } else{
                        Log.v(TAG, "Response VACIA PATCH: ");

                        if(isReserved){
                            showDialogResult(Menu.this, true , "Entrada Reservada\nFalta cobrar entrada", "Dorsal: " + id + "\n" + isInmortals
                                            + "Nombre: " + name + "\n" +
                                            "Email: " + email + "\n" +
                                            "DNI: " + dni + "\n" +
                                            "FechaCompra: " + date.split("T")[0] + "\n" +
                                            "HoraCompra: " + date.split("T")[1].split("\\.")[0]
                                    , "Ok").show();
                        }else{
                            showDialogResult(Menu.this, true , "Entrada Correcta\nDorsal: " + id, isInmortals + "Nombre: " + name + "\n" +
                                            "Email: " + email + "\n" +
                                            "DNI: " + dni + "\n" +
                                            "FechaCompra: " + date.split("T")[0] + "\n" +
                                            "HoraCompra: " + date.split("T")[1].split("\\.")[0]
                                    , "Ok").show();
                        }

                    }

                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> mHeaders = new ArrayMap<String, String>();
                    mHeaders.put("authorization", "Bearer " + token);
                    mHeaders.put("Content-Type", "application/json");
                    return mHeaders;
                }
            };
            queue.add(jsonArrayRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private AlertDialog showDialogResult(final Activity act, boolean isOk, CharSequence title, CharSequence message, CharSequence buttonYes) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);
        if(isOk){
            downloadDialog.setIcon(R.mipmap.ic_gtick);
        }else{
            downloadDialog.setIcon(R.mipmap.ic_redcross);
        }
        downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        downloadDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                scanQR(null);
            }
        });
        return downloadDialog.show();
    }

}
