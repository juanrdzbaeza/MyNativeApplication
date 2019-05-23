package com.juanrdzbaeza.mynativeapplication;

import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
  int h[][] = new int[3][256]; //array con el histograma

  Button calculate_button;
  CheckBox asyncTaskBox;
  String calculating = "Calculating...";
  TextView tv;
  TextView ya;
  EditText hilos;
  EditText tareas;
  Integer nHilos;
  Integer nTareas;

  double tiempoInicio, tiempoFin, tiempoTotal;

  int tam = 1000; // Imagen de 1.000 x 1.000 pixeles RGB
  short imagen[][][] = new short[3][tam][tam];

  // Used to load the 'native-lib' library on application startup.

  /**
   * madre del cordero
   */
  static {
    System.loadLibrary("native-lib");
  }


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);


    asyncTaskBox        = (CheckBox)findViewById(R.id.asinc);
    calculate_button    = (Button)findViewById(R.id.button);
    tv                  = (TextView)findViewById(R.id.sample_text);
    ya                  = (TextView)findViewById(R.id.ya);
    hilos               = (EditText)findViewById(R.id.hilostext);
    tareas              = (EditText)findViewById((R.id.tareastext));

    calculate_button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        tv.setText(calculating);
        tv.setTextColor(Color.GRAY);
        ya.setVisibility(View.INVISIBLE);
        // Ejemplo de la llamada a un método nativo

        if (hilos.getText().toString().matches("")) {
          nHilos = 0;
        }else{
          nHilos = Integer.valueOf(hilos.getText().toString());
        }
        if (tareas.getText().toString().matches("")){
          nTareas = 1;
        } else {
          nTareas = Integer.valueOf(tareas.getText().toString());
        }

        calculate_button.setEnabled(false);
        if (!asyncTaskBox.isChecked()) {

          tiempoInicio = System.nanoTime();
          tiempoFin = histograma(nHilos, nTareas);
          tiempoTotal = (tiempoFin - tiempoInicio) / 1000000000.0;
          calculate_button.setEnabled(true);

          String stringtiempo = String.valueOf(tiempoTotal); //Paso el resultado a string
          tv.setText(stringtiempo + " seg.");
          tv.setTextColor(Color.RED);
          ya.setVisibility(View.VISIBLE);

        } else {  // lanzar tarea asincrona para hacer el trabajo
          new myAsyncTask().execute();

        }
        //String stringtiempo = String.valueOf(tiempo); //Paso el resultado a string
        //tv.setText(stringtiempo + " seg.");
      }
    });

  }

    /*public double histograma(int tam, short[][][] imagen) {
        double tiempoInicio = System.nanoTime();
        for (int i = 0; i < tam; i++)
            for (int j = 0; j < tam; j++)
                for (int k = 0; k < 3; k++)
                    h[k][imagen[k][i][j]]++; // Contabiliza el número de veces que aparece cada valor.
        return (System.nanoTime() - tiempoInicio) / 1000000000.0;
    }*/

  // int tam, short[][][] imagen

  public double histograma(Integer taskId, Integer nTasks) {
    long aux = 0;
    int ini = (int)((long)taskId*tam/nTasks);
    int fin = (int)((long)(taskId+1)*tam/nTasks);

    for (int i = 0; i < 256; i++) // Inicializa el histograma.
      for (int k = 0; k < 3; k++) h[k][i] = 0;
    for (int i = 0; i < tam; i++) // Inicializa la imagen.
      for (int j = 0; j < tam; j++)
        for (int k = 0; k < 3; k++) imagen[k][i][j] = (short) ((i * j) % 256);
    for (int i = 0; i < tam; i++) // Contabiliza el nº veces que aparece cada valor.
      for (int j = 0; j < tam; j++)
        for (int k = 0; k < 3; k++) h[k][imagen[k][i][j]]++;

    for (int i = ini; i < fin; i++) { // Modificar imagen utilizando el histograma
      for (int j = 0; j < tam; j++)
        for (int k = 0; k < 3; k++){
          for (int x = 0; x < 256; x++) {
            aux = (long) (h[k][x] * h[k][x] * h[k][x] * h[k][x] - h[k][x] * h[k][x] * h[k][x] + h[k][x] * h[k][x]);
            h[k][x]= (int) (aux % 256);
          }
          imagen[k][i][j] = (short)(imagen[k][i][j] * h[k][imagen[k][i][j]]);
        }
    }
    return System.nanoTime();
  }


  // Tarea asincrona
  private class myAsyncTask extends AsyncTask<Void,Void,Void> {
    // Ejemplo de la llamada a un método nativo
    @Override
    protected Void doInBackground(Void... v) {
      tiempoInicio = System.nanoTime();
      // se hace el trabajo en el thread de background
      tiempoFin = histograma(nHilos, nTareas);

      return null;
    }
    @Override
    protected void onPostExecute(Void voids) {
      // al terminar, en el thread UI se actualizan los elementos de la pantalla
      calculate_button.setEnabled(true);
      tiempoTotal = (tiempoFin - tiempoInicio) / 1000000000.0;
      String stringtiempo = String.valueOf(tiempoTotal); //Pasa a string
      tv.setText(stringtiempo + " seg.");
      tv.setTextColor(Color.RED);
      ya.setVisibility(View.VISIBLE);
    }
  }
}
