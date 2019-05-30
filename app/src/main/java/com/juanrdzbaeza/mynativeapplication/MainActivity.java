package com.juanrdzbaeza.mynativeapplication;

import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
  int h[][] = new int[3][256]; //array con el histograma

  Button calculate_button;
  CheckBox asyncTaskBox;
  RadioButton threadsButton, executorButton, cppjniButton;
  String calculating = "Calculating...";
  TextView tv, nv;
  EditText hilos, tareas;
  Integer nHilos, nTareas /*nVuelta = 0*/;

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

    threadsButton       = (RadioButton)findViewById(R.id.javathreadradiobutton);
    executorButton      = (RadioButton)findViewById(R.id.executorradiobutton);
    cppjniButton        = (RadioButton) findViewById(R.id.cppjniradiobutton);
    asyncTaskBox        = (CheckBox)findViewById(R.id.asinc);
    calculate_button    = (Button)findViewById(R.id.button);
    tv                  = (TextView)findViewById(R.id.sample_text);
    nv                  = (TextView)findViewById(R.id.nv);
    hilos               = (EditText)findViewById(R.id.hilostext);
    tareas              = (EditText)findViewById((R.id.tareastext));
    //nVuelta = 0;

    // No se desea lanzar hebras si no está activa la tarea asíncrona.
    threadsButton.setEnabled(false);
    executorButton.setEnabled(false);
    cppjniButton.setEnabled(false);
    hilos.setEnabled(false);
    tareas.setEnabled(false);

    // Habilitar las hebras Java solo cuando se active tarea asincrona
    asyncTaskBox.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if(asyncTaskBox.isChecked()) {
          threadsButton.setEnabled(true);
          executorButton.setEnabled(true);
          cppjniButton.setEnabled(true);
          hilos.setEnabled(true);
          tareas.setEnabled(true);
        }
        else {
          threadsButton.setEnabled(false);
          executorButton.setEnabled(false);
          cppjniButton.setEnabled(false);
          hilos.setEnabled(false);
          tareas.setEnabled(false);
        }
      }
    });

    calculate_button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        tv.setText(calculating);
        nv.setText(calculating);
        tv.setTextColor(Color.GRAY);
        nv.setTextColor(Color.GRAY);
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
          //nv.setText(String.valueOf(nVuelta));
          //nv.setTextColor(Color.BLUE);
          //nVuelta = 0;

        } else {
          // lanzar tarea asincrona para hacer el trabajo
          new myAsyncTask().execute();

        }
        //String stringtiempo = String.valueOf(tiempo); //Paso el resultado a string
        //tv.setText(stringtiempo + " seg.");
      }
    });

  }

  public double histograma(Integer taskId, Integer nTasks) {
    long aux = 0;
    int ini = (int)((long)taskId*tam/nTasks);     // => 3*1000/4 = 750
    int fin = (int)((long)(taskId+1)*tam/nTasks); // => (3+1)*1000/4 = 1000

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
            // nVuelta++;
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
      if (threadsButton.isChecked()) {
        // hacer el trabajo usando Java Threads
        tiempoInicio = System.nanoTime();
        doThreadWork();
        tiempoFin = System.nanoTime();

      } else if (executorButton.isChecked()) {
        // hacer el trabajo usando un Executor
        tiempoInicio = System.nanoTime();
        doExecutorWork();
        tiempoFin = System.nanoTime();

      } else if (cppjniButton.isChecked()) {
        // hacer el trabajo usando C++ JNI
        int hvector[] = new int[3*256]; //vector con el histograma
        short imagenvector[] = new short[3*tam*tam];
        tiempoInicio = System.nanoTime();
        histogramaC(tam, imagenvector, hvector);
        tiempoFin = System.nanoTime();

      } else { // se hace el trabajo en el thread de background
        tiempoInicio = System.nanoTime();
        tiempoFin = histograma(0,1);

      }

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
      //nv.setText(String.valueOf(nVuelta));
      //nVuelta = 0;
    }
  }

  // clase que representa las tareas, su método "run" es el que realiza el trabajo efectivo
  public class myTask implements Runnable {
    private int myId;
    private int nTasks;
    Double tiempoAuxiliar;
    myTask(int _myId, int _nTasks) { myId = _myId; nTasks = _nTasks; }

    // método para hacer el trabajo de la tarea
    @Override
    public void run() {
      tiempoAuxiliar = histograma(myId, nTasks);
    }
  }

  // metodo para hacer el trabajo con Java Threads directamente
  public void doThreadWork() {
    // hacer el trabajo usando Java Threads
    int numtasks = nTareas;
    int nThreads = nHilos;
    if (numtasks > tam) numtasks = tam;

    Thread[] threads = new Thread[nThreads];
    int count = 0;
    while (count < numtasks) {
      /**
       * Se van a crear tantos threads como tareas especificadas
       * pero en tandas de nThreads cada vez
       */
      int nth = 0;
      while (nth < nThreads && count < numtasks) {
        // se crean nThreads y se ponen a trabajar (start)
        threads[nth] = new Thread(new myTask(count, numtasks));
        threads[nth].start();
        nth++;
        count++;
      }
      for (int i = 0; i < nth; i++) {
        // se espera a que terminen los nThreads que están trabajando antes de crear otros
        try {
          threads[i].join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  } // fin doThreadWork()

  // metodo para hacer el trabajo usando Executor (FixedThreadPool)
  public void doExecutorWork() {
    // se lee el numero de tareas especificado por el usuario
    int numtasks = nTareas;
    int nThreads = nHilos;
    if (numtasks > tam) numtasks = tam;

    /**
     * se crea un executor de typo FixedThreadPool con tantos threads
     * como numero de procesadores
     */
    ExecutorService executor = Executors.newFixedThreadPool(nThreads);
    // se crean todas las tasks indicadas y se mandan a ejecutar en el executor
    for (int i=0; i < numtasks; i++) {
      executor.execute(new myTask(i, numtasks));
    }
    // se para el Executor para que no acepte mas tareas y termine las lanzadas
    executor.shutdown();
    try {
      // se espera a que terminen todas las tareas pendientes en el Executor
      executor.awaitTermination(10, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  } // fin fin doExecutorWork()


  public native void histogramaC(int tam, short[] imagen, int... h);
}
