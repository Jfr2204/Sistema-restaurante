package com.mycompany.helloworld.ejemplopostgresql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;


public class MainFrame extends javax.swing.JFrame {

    //Valores para la conexión a la base de datos (su nombre, URL, Usuario y Contraseña).
    private static final String DB_NAME = "RestoFreaky";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/" + DB_NAME;
    private static final String DB_USER = "postgres";
    private static final String DB_PWD = "admin";
    
    //Mensajes de error.
    private static final String ERROR_MSG_INSERT = "Error al intentar dar de alta a esta persona.";
    private static final String ERROR_MSG_INSERT_INPUT = "No se admiten campos vacíos.";
    private static final String ERROR_MSG_INSERT_CODE = "El Código debe ser mayor a cero.";
    private static final String ERROR_MSG_INSERT_POSITIVE = "No se admiten numeros negativos.";
    private static final String ERROR_MSG_INSERT_CODE_EXISTS = "El Código ingresado pertenece a un plato ya existente en la base de datos.";
    private static final String ERROR_MSG_INSERT_FORMAT = "Solo se permiten números para el precio";
    private static final String ERROR_MSG_DELETE_EXISTS = "El mozo que desea eliminar no existe en la base de datos.";
    private static final String ERROR_MSG_DELETE_TABLE = "El mozo que desea eliminar tiene una mesa asignada.";
    private static final String ERROR_MSG_CONSULT_EMPTY = "Ingrese un código";
    private static final String ERROR_MSG_CONSULT_WAITER = "El mozo consultado no existe en la base de datos o no tiene mesas asignadas.";
    private static final String ERROR_MSG_CONSULT_TABLE = "La mesa consultada no existe en la base de datos.";
    private static final String ERROR_MSG_CONSULT_DATE1 = "El formato ingresado es incorrecto";
    private static final String ERROR_MSG_CONSULT_DATE2 = "La primera fecha debe ser menor que la segunda fecha";
    
    // Objetos utilizados para interactuar con la base de datos: (conexión, realizar consultas con y sin parámetros, y recibir los resultados).
    private static Connection conn = null;
    private static Statement query = null;
    private static PreparedStatement p_query = null;
    private static ResultSet result = null;


    /**
     * Creates new form MainFrame
     */
    
    public MainFrame() throws SQLException {
        initComponents();
        this.setLocationRelativeTo(null);
        this.setResizable(false);
        setTitle("RestoFreaky Manager");
        label_error.setVisible(false);
        //Conexión con la base de datos.
        conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PWD);
        //Creación de las tablas si estas no existen.
        creacionTablas();
        
        //Control para saber si es la primera ejecución
        int cont = 0;
        PreparedStatement p_queryControl = null;
        ResultSet  resultControl = null;
        p_queryControl = (PreparedStatement) conn.prepareStatement("SELECT * FROM mozos");
        resultControl = p_queryControl.executeQuery();
        while(resultControl.next()){  //Sacar la iteracion.
            cont++;
        }
        if(cont == 0){
            query.execute(leerArchivo());
            primeraEjecucion();
        }
        
        //Invocación para que muestre los mozos ni bien se ejcuta el programa
        updateLista("SELECT Mo_Cod AS \"Código\", Mo_NombreApellido AS \"Nombre\", Mo_Domicilio AS \"Domicilio\", Mo_DNI AS \"DNI\" FROM mozos");
        inicializarPestañaConsultaConParametros();
        inicializarPestañaResumenDisponibilidad();
        
    }
    
     private void creacionTablas() throws SQLException {
        query = conn.createStatement();
        query.execute("CREATE TABLE IF NOT EXISTS mozos("
            + "Mo_Cod INT NOT NULL,"
            + "Mo_NombreApellido VARCHAR(100) NOT NULL,"
            + "Mo_Domicilio VARCHAR(100) NOT NULL,"
            + "Mo_DNI INT NOT NULL UNIQUE,"
            + "PRIMARY KEY (Mo_Cod))");
        query.execute("CREATE TABLE IF NOT EXISTS platos("
            + "P_Cod INT NOT NULL,"
            + "P_Nombre VARCHAR(100) NOT NULL,"
            + "P_Descripcion VARCHAR(600) NOT NULL,"
            + "P_Tipo VARCHAR(100) NOT NULL,"
            + "P_PrecioCosto FLOAT NOT NULL,"
            + "P_PrecioVenta FLOAT NOT NULL,"
            + "P_PrecioPromocion FLOAT NOT NULL,"
            + "PRIMARY KEY (P_Cod))");
        query.execute("CREATE TABLE IF NOT EXISTS mesas("
            + "Me_Cod INT NOT NULL,"
            + "Me_Sector VARCHAR(100) NOT NULL,"
            + "Mo_Cod_Atiende INT NOT NULL,"
            + "PRIMARY KEY (Me_Cod),"
            + "FOREIGN KEY (Mo_Cod_Atiende) REFERENCES mozos(Mo_Cod))");
        query.execute("CREATE TABLE IF NOT EXISTS consumos("
            + "C_Cod INT NOT NULL,"
            + "C_Fecha CHAR(10) NOT NULL,"
            + "C_Hora CHAR(5) NOT NULL,"
            + "Me_Cod_Realiza INT NOT NULL,"
            + "PRIMARY KEY (C_Cod),"
            + "FOREIGN KEY (Me_Cod_Realiza) REFERENCES mesas(Me_Cod))");
         query.execute("CREATE TABLE IF NOT EXISTS se_consume("
            + "C_Cod INT NOT NULL,"
            + "P_Cod INT NOT NULL,"
            + "PRIMARY KEY (C_Cod, P_Cod),"
            + "FOREIGN KEY (C_Cod) REFERENCES consumos(C_Cod),"
            + "FOREIGN KEY (P_Cod) REFERENCES platos(P_Cod))");
    }
    
    private String leerArchivo() {
        File f = new File("src/test/Archivos/Inserta_Datos.sql.txt");
        String linea;
        StringBuffer sb = new StringBuffer();
        try {
            FileInputStream fis = new FileInputStream(f);
            InputStreamReader isr = new InputStreamReader(fis, "UTF8");
            BufferedReader br = new BufferedReader(isr);
            while((linea = br.readLine()) != null){
                sb.append(linea + "\n");
            }
            br.close();   
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "No se encontró  el archivo de carga de datos");
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }
    
    private void primeraEjecucion() throws SQLException {
            int codigoMozo;
            updateLista("SELECT MAX(Mo_Cod) FROM mozos");
            codigoMozo = (int) jTablaConsultas.getModel().getValueAt(0, 0) + 1;
            p_query = conn.prepareStatement("INSERT\n"
                                            + "INTO mozos\n"
                                            + "VALUES(? ,'Krusty','Aristobulo del Valle 3432',25544555)");
            p_query.setInt(1, codigoMozo);
            p_query.executeUpdate();
            query.execute("UPDATE platos\n" 
                          + "SET P_PrecioVenta = 690\n"
                          + "WHERE P_Nombre = 'Semillas Senzu'");
  
            query.execute("DELETE\n"
                          + "FROM mozos\n"
                          + "WHERE Mo_NombreApellido = 'Jesse Pinkman'");
    }
    
    private void updateForm() throws SQLException {
        // actualizar y limpiar el formulario luego de una operación exitosa
        jCodigo.setValue(0);
        jNombre.setText("");
        jDescripcion.setText("");
        jTipo.setSelectedIndex(0);
        jPrecioCosto.setText("");
        jPrecioVenta.setText("");
        jPrecioPromocion.setText("");
        jEliminarPorCodigo.setValue(0);
        jParametroConsulta.setValue(0);
        label_error.setVisible(false);
    }
    
    private void inicializarPestañaConsultaConParametros(){
        jCartelCodigo.setVisible(true);
        jParametroConsulta.setVisible(true);
        jCartelPrimeraFecha.setVisible(false);
        jCartelSegundaFecha.setVisible(false);
        jPrimeraFecha.setVisible(false);
        jSegundaFecha.setVisible(false);
    }
    
    private void inicializarPestañaResumenDisponibilidad(){
        try {
            query = conn.createStatement();
            result = query.executeQuery("SELECT COUNT(*) AS \"Cantidad de mozos\""
                                        + "FROM mozos");
            jTablaCantidadMozosDisponibles.setModel(resultToTable(result));
            result = query.executeQuery("SELECT COUNT(*) AS \"Cantidad de mesas\""
                                        + "FROM mesas"); 
            jTablaCantidadMesas.setModel(resultToTable(result));
            result = query.executeQuery("SELECT COUNT(*) AS \"Cantidad de entradas\""
                                        + "FROM platos "
                                        + "WHERE P_Tipo = 'Entrada'"); 
            jTablaCantidadEntradas.setModel(resultToTable(result));
            result = query.executeQuery("SELECT COUNT(*) AS \"Cantidad de platos principales\""
                                        + "FROM platos "
                                        + "WHERE P_Tipo = 'Plato Principal'"); 
            jTablaCantidadPlatosPrincipales.setModel(resultToTable(result));
            result = query.executeQuery("SELECT COUNT(*) AS \"Cantidad de postres\""
                                        + "FROM platos "
                                        + "WHERE P_Tipo = 'Postre'"); 
            jTablaCantidadPostres.setModel(resultToTable(result));
        } catch (SQLException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void ocultarCartelExito(JLabel label) {
        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            SwingUtilities.invokeLater(() -> label.setVisible(false));
        }).start();
    }

    private void updateLista(String consulta) throws SQLException {
        query = conn.createStatement();
        result = query.executeQuery(consulta);
        jTablaConsultas.setModel(resultToTable(result));
    }
    
    private void updateListaConParametros(String consulta, int parametro) throws SQLException {
        p_query = conn.prepareStatement(consulta);
        p_query.setInt(1, (int) parametro);
        result = p_query.executeQuery();
        jTablaConsultasParametros.setModel(resultToTable(result));
    }

    private void updateListaFecha(String primeraFecha, String segundaFecha) throws SQLException{
        SimpleDateFormat formato;
        formato = new SimpleDateFormat("yyyy/MM/dd");
        try {
            java.util.Date setFormato;
            setFormato = formato.parse(primeraFecha);
            java.sql.Date sqlPrimeraFecha = new java.sql.Date(setFormato.getTime());
            setFormato = formato.parse(segundaFecha);
            java.sql.Date sqlSegundaFecha = new java.sql.Date(setFormato.getTime());
            if(sqlPrimeraFecha.after(sqlSegundaFecha)){
                JOptionPane.showMessageDialog(this, ERROR_MSG_CONSULT_DATE2);
            }  
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, ERROR_MSG_CONSULT_DATE1);
            return;
        }
        p_query = conn.prepareStatement("SELECT DISTINCT platos.P_Cod AS \"Codigo del Plato\",P_Nombre AS \"Nombre del Plato\" "
                                        + "FROM platos,se_consume,consumos WHERE se_consume.C_Cod = consumos.C_Cod AND platos.P_Cod = se_consume.P_Cod AND (C_Fecha BETWEEN ? AND ?) "
                                        + "ORDER BY platos.P_Cod");
        p_query.setString(1, primeraFecha);
        p_query.setString(2, segundaFecha);
        result = p_query.executeQuery();
        jTablaConsultasParametros.setModel(resultToTable(result));
    }

    private static DefaultTableModel resultToTable(ResultSet rs) throws SQLException {
        // Esta es una función auxiliar que les permite convertir los resultados de las
        // consultas (ResultSet) a un modelo interpretable para la tabla mostrada en pantalla
        ResultSetMetaData metaData = rs.getMetaData();

        // creando las colummnas de la tabla
        Vector <String> columnNames = new Vector<String>();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(metaData.getColumnName(i));
        }

        // creando las filas de la tabla con los resultados de la consulta
        Vector<Vector<Object>> data = new Vector<Vector<Object>>();
        while (rs.next()) {
            Vector<Object> vector = new Vector<Object>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                vector.add(rs.getObject(columnIndex));
            }
            data.add(vector);
        }

        return new DefaultTableModel(data, columnNames);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanelConsultas = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTablaConsultas = new javax.swing.JTable();
        jLabel9 = new javax.swing.JLabel();
        jComboConsultas = new javax.swing.JComboBox<>();
        jBotonMostrar1 = new javax.swing.JButton();
        jPanelConsultasParametros = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jComboConsultasParametros = new javax.swing.JComboBox<>();
        jCartelCodigo = new javax.swing.JLabel();
        jParametroConsulta = new javax.swing.JSpinner();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTablaConsultasParametros = new javax.swing.JTable();
        jBotonMostrar2 = new javax.swing.JButton();
        jPrimeraFecha = new javax.swing.JTextField();
        jSegundaFecha = new javax.swing.JTextField();
        jCartelPrimeraFecha = new javax.swing.JLabel();
        jCartelSegundaFecha = new javax.swing.JLabel();
        jPanelInsertar = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jNombre = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jbInsertar = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jDescripcion = new javax.swing.JTextField();
        jTipo = new javax.swing.JComboBox<>();
        jCodigo = new javax.swing.JSpinner();
        jPrecioCosto = new javax.swing.JTextField();
        jPrecioVenta = new javax.swing.JTextField();
        jPrecioPromocion = new javax.swing.JTextField();
        jPanelEliminar = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jEliminarPorCodigo = new javax.swing.JSpinner();
        jbEliminar = new javax.swing.JButton();
        jPanelResumen = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTablaCantidadMozosDisponibles = new javax.swing.JTable();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTablaCantidadPostres = new javax.swing.JTable();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTablaCantidadPlatosPrincipales = new javax.swing.JTable();
        jScrollPane7 = new javax.swing.JScrollPane();
        jTablaCantidadEntradas = new javax.swing.JTable();
        jScrollPane8 = new javax.swing.JScrollPane();
        jTablaCantidadMesas = new javax.swing.JTable();
        jBotonActualizar = new javax.swing.JButton();
        label_error = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Base de Datos (Ejemplo Básico de Conexión e Interacción)");

        jTablaConsultas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Columna 1", "Columna 2", "Columna 3", "Columna 4"
            }
        ));
        jScrollPane1.setViewportView(jTablaConsultas);
        jTablaConsultas.getAccessibleContext().setAccessibleName("");
        jTablaConsultas.getAccessibleContext().setAccessibleDescription("");

        jLabel9.setText("Consulta:");

        jComboConsultas.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Mozos", "Platos", "Cantidad de mesas por mozo", "Platos más consumidos", "Mozos libres", "Máximo mínimo y promedio de platos principales", "Platos nunca consumidos" }));
        jComboConsultas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboConsultasActionPerformed(evt);
            }
        });

        jBotonMostrar1.setText("Mostrar");
        jBotonMostrar1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBotonMostrar1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelConsultasLayout = new javax.swing.GroupLayout(jPanelConsultas);
        jPanelConsultas.setLayout(jPanelConsultasLayout);
        jPanelConsultasLayout.setHorizontalGroup(
            jPanelConsultasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelConsultasLayout.createSequentialGroup()
                .addGroup(jPanelConsultasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelConsultasLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 745, Short.MAX_VALUE))
                    .addGroup(jPanelConsultasLayout.createSequentialGroup()
                        .addGap(135, 135, 135)
                        .addComponent(jLabel9)
                        .addGap(18, 18, 18)
                        .addComponent(jComboConsultas, javax.swing.GroupLayout.PREFERRED_SIZE, 314, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jBotonMostrar1)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelConsultasLayout.setVerticalGroup(
            jPanelConsultasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelConsultasLayout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(jPanelConsultasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jComboConsultas, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBotonMostrar1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 31, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 197, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21))
        );

        jTabbedPane1.addTab("Consultas", jPanelConsultas);

        jLabel10.setText("Mostrar:");

        jComboConsultasParametros.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Mesas asignadas a un mozo", "Platos consumidos en una mesa", "Platos consumidos entre dos fechas", "Cantidad de platos consumidos en una mesa" }));
        jComboConsultasParametros.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboConsultasParametrosItemStateChanged(evt);
            }
        });

        jCartelCodigo.setText("Ingrese el código:");

        jTablaConsultasParametros.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Columna 1", "Columna 2", "Columna 3", "Columna 4"
            }
        ));
        jScrollPane2.setViewportView(jTablaConsultasParametros);

        jBotonMostrar2.setText("Mostrar");
        jBotonMostrar2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBotonMostrar2ActionPerformed(evt);
            }
        });

        jCartelPrimeraFecha.setText("Ingrese la primera Fecha:");

        jCartelSegundaFecha.setText("Ingrese la segunda Fecha:");

        javax.swing.GroupLayout jPanelConsultasParametrosLayout = new javax.swing.GroupLayout(jPanelConsultasParametros);
        jPanelConsultasParametros.setLayout(jPanelConsultasParametrosLayout);
        jPanelConsultasParametrosLayout.setHorizontalGroup(
            jPanelConsultasParametrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelConsultasParametrosLayout.createSequentialGroup()
                .addGroup(jPanelConsultasParametrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelConsultasParametrosLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane2))
                    .addGroup(jPanelConsultasParametrosLayout.createSequentialGroup()
                        .addGroup(jPanelConsultasParametrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelConsultasParametrosLayout.createSequentialGroup()
                                .addGroup(jPanelConsultasParametrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanelConsultasParametrosLayout.createSequentialGroup()
                                        .addGap(85, 85, 85)
                                        .addComponent(jCartelPrimeraFecha)
                                        .addGap(18, 18, 18)
                                        .addComponent(jPrimeraFecha, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(69, 69, 69)
                                        .addComponent(jCartelSegundaFecha))
                                    .addGroup(jPanelConsultasParametrosLayout.createSequentialGroup()
                                        .addGap(230, 230, 230)
                                        .addComponent(jCartelCodigo)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jParametroConsulta, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jSegundaFecha, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanelConsultasParametrosLayout.createSequentialGroup()
                                .addGap(150, 150, 150)
                                .addComponent(jLabel10)
                                .addGap(35, 35, 35)
                                .addComponent(jComboConsultasParametros, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jBotonMostrar2)))
                        .addGap(0, 147, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelConsultasParametrosLayout.setVerticalGroup(
            jPanelConsultasParametrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelConsultasParametrosLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelConsultasParametrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBotonMostrar2)
                    .addComponent(jComboConsultasParametros, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelConsultasParametrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCartelPrimeraFecha)
                    .addComponent(jPrimeraFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSegundaFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCartelSegundaFecha))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanelConsultasParametrosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jParametroConsulta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCartelCodigo))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Consultas con parámetros", jPanelConsultasParametros);

        jLabel1.setText("Código:");

        jLabel2.setText("Nombre:");

        jLabel3.setText("Precio costo:");

        jbInsertar.setText("Insertar Plato");
        jbInsertar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbInsertarActionPerformed(evt);
            }
        });

        jLabel5.setText("Descripción:");

        jLabel6.setText("Tipo:");

        jLabel7.setText("Precio venta:");

        jLabel8.setText("Precio promoción:");

        jTipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Entrada", "Plato principal", "Postre" }));

        javax.swing.GroupLayout jPanelInsertarLayout = new javax.swing.GroupLayout(jPanelInsertar);
        jPanelInsertar.setLayout(jPanelInsertarLayout);
        jPanelInsertarLayout.setHorizontalGroup(
            jPanelInsertarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInsertarLayout.createSequentialGroup()
                .addGroup(jPanelInsertarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanelInsertarLayout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addComponent(jLabel5)
                        .addGap(18, 18, 18)
                        .addComponent(jDescripcion, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelInsertarLayout.createSequentialGroup()
                        .addGap(71, 71, 71)
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(jCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelInsertarLayout.createSequentialGroup()
                        .addGap(67, 67, 67)
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(jNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanelInsertarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelInsertarLayout.createSequentialGroup()
                        .addGap(160, 160, 160)
                        .addComponent(jLabel8)
                        .addGap(18, 18, 18)
                        .addComponent(jPrecioPromocion, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 1, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelInsertarLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanelInsertarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelInsertarLayout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addGap(18, 18, 18)
                                .addComponent(jTipo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelInsertarLayout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(18, 18, 18)
                                .addComponent(jPrecioCosto, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelInsertarLayout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addGap(18, 18, 18)
                                .addComponent(jPrecioVenta, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addGap(0, 142, Short.MAX_VALUE))
            .addGroup(jPanelInsertarLayout.createSequentialGroup()
                .addGap(328, 328, 328)
                .addComponent(jbInsertar)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelInsertarLayout.setVerticalGroup(
            jPanelInsertarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInsertarLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInsertarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel6)
                    .addComponent(jTipo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelInsertarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(jPrecioCosto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanelInsertarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelInsertarLayout.createSequentialGroup()
                        .addGroup(jPanelInsertarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(jPrecioVenta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanelInsertarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(jPrecioPromocion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLabel5)
                    .addComponent(jDescripcion, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
                .addComponent(jbInsertar)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Insertar plato", jPanelInsertar);

        jLabel4.setText("Código:");

        jbEliminar.setText("Eliminar");
        jbEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbEliminarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelEliminarLayout = new javax.swing.GroupLayout(jPanelEliminar);
        jPanelEliminar.setLayout(jPanelEliminarLayout);
        jPanelEliminarLayout.setHorizontalGroup(
            jPanelEliminarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelEliminarLayout.createSequentialGroup()
                .addGroup(jPanelEliminarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelEliminarLayout.createSequentialGroup()
                        .addGap(207, 207, 207)
                        .addComponent(jLabel4)
                        .addGap(18, 18, 18)
                        .addComponent(jEliminarPorCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelEliminarLayout.createSequentialGroup()
                        .addGap(340, 340, 340)
                        .addComponent(jbEliminar)))
                .addContainerGap(275, Short.MAX_VALUE))
        );
        jPanelEliminarLayout.setVerticalGroup(
            jPanelEliminarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelEliminarLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(jPanelEliminarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jEliminarPorCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 214, Short.MAX_VALUE)
                .addComponent(jbEliminar)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Eliminar mozo", jPanelEliminar);

        jTablaCantidadMozosDisponibles.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane4.setViewportView(jTablaCantidadMozosDisponibles);

        jTablaCantidadPostres.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane5.setViewportView(jTablaCantidadPostres);

        jTablaCantidadPlatosPrincipales.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane6.setViewportView(jTablaCantidadPlatosPrincipales);

        jTablaCantidadEntradas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane7.setViewportView(jTablaCantidadEntradas);

        jTablaCantidadMesas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane8.setViewportView(jTablaCantidadMesas);

        jBotonActualizar.setText("Actualizar");
        jBotonActualizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBotonActualizarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelResumenLayout = new javax.swing.GroupLayout(jPanelResumen);
        jPanelResumen.setLayout(jPanelResumenLayout);
        jPanelResumenLayout.setHorizontalGroup(
            jPanelResumenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelResumenLayout.createSequentialGroup()
                .addContainerGap(65, Short.MAX_VALUE)
                .addGroup(jPanelResumenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanelResumenLayout.createSequentialGroup()
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 189, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(47, 47, 47)
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 191, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelResumenLayout.createSequentialGroup()
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 189, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jScrollPane8, javax.swing.GroupLayout.PREFERRED_SIZE, 191, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(36, 36, 36)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 191, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(46, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelResumenLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jBotonActualizar)
                .addGap(340, 340, 340))
        );
        jPanelResumenLayout.setVerticalGroup(
            jPanelResumenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelResumenLayout.createSequentialGroup()
                .addGap(0, 66, Short.MAX_VALUE)
                .addGroup(jPanelResumenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane8, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(41, 41, 41)
                .addGroup(jPanelResumenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 68, Short.MAX_VALUE)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(jBotonActualizar)
                .addGap(12, 12, 12))
        );

        jTabbedPane1.addTab("Resumen de disponibilidad", jPanelResumen);

        label_error.setForeground(new java.awt.Color(0, 204, 0));
        label_error.setText("Éxito!");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(label_error, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 326, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(label_error)
                .addContainerGap())
        );

        jTabbedPane1.getAccessibleContext().setAccessibleName("tab_panel");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    private void jbInsertarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbInsertarActionPerformed
        if(jNombre.getText().trim().equals("") || jDescripcion.getText().trim().equals("")) {
            JOptionPane.showMessageDialog(this, ERROR_MSG_INSERT_INPUT);
            return;
        }
        if((int)jCodigo.getValue() <= 0){
            JOptionPane.showMessageDialog(this, ERROR_MSG_INSERT_CODE);
            return;
        }
        try {
            int i;
            float pCosto, pVenta, pPromocion;
            Boolean cond = false;
            updateLista("SELECT P_Cod FROM platos");
            for(i = 0; i < jTablaConsultas.getRowCount(); i++){
                if((int)jTablaConsultas.getModel().getValueAt(i, 0) == (int)jCodigo.getValue())
                    cond = true;
            }
            if(cond == false){
                p_query = conn.prepareStatement("INSERT INTO platos VALUES (?, ?, ?, ?, ?, ?, ?)");
                p_query.setInt(1, (int) jCodigo.getValue());       
                p_query.setString(2, jNombre.getText().trim()); 
                p_query.setString(3, jDescripcion.getText().trim()); 
                p_query.setString(4, (String)jTipo.getSelectedItem());
                pCosto = Float.parseFloat(jPrecioCosto.getText());
                pVenta = Float.parseFloat(jPrecioVenta.getText());
                pPromocion = Float.parseFloat(jPrecioPromocion.getText());
                if(pCosto < 0.0 || pVenta < 0.0 || pPromocion < 0.0){
                    JOptionPane.showMessageDialog(this, ERROR_MSG_INSERT_POSITIVE);
                    return;
                }
                p_query.setFloat(5, pCosto);
                p_query.setFloat(6, pVenta);
                p_query.setFloat(7, pPromocion);
                p_query.executeUpdate();
                updateForm();
                updateLista("SELECT P_Cod AS \"Código\", P_Nombre AS \"Nombre\", P_Descripcion AS \"Descripción\", P_Tipo AS \"Tipo\", P_PrecioCosto AS \"Precio de costo\", "  
                                + "P_PrecioVenta AS \"Precio de venta\", P_PrecioPromocion AS \"Precio de promocion\" FROM platos");
                label_error.setVisible(true);
                ocultarCartelExito(label_error);
            }
            else{
                JOptionPane.showMessageDialog(this, ERROR_MSG_INSERT_CODE_EXISTS);
            }
        } catch (SQLException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
            label_error.setText(ERROR_MSG_INSERT);
            label_error.setVisible(true);
        } catch (NumberFormatException exn){
            JOptionPane.showMessageDialog(this, ERROR_MSG_INSERT_FORMAT);
        }
    }//GEN-LAST:event_jbInsertarActionPerformed

    private void jbEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbEliminarActionPerformed
        try{
            Boolean cond1, cond2;
            cond1 = false;
            cond2 = true;
            int i;
            updateLista("SELECT Mo_Cod FROM mozos");
            for(i = 0; i < jTablaConsultas.getRowCount(); i++){
                if((int)jTablaConsultas.getModel().getValueAt(i, 0) == (int)jEliminarPorCodigo.getValue())
                    cond1 = true;
            }
            if(cond1 == true){
                updateLista("SELECT Mo_Cod_Atiende FROM mesas");
                for(i = 0; i < jTablaConsultas.getRowCount(); i++){
                    if((int)jTablaConsultas.getModel().getValueAt(i, 0) == (int)jEliminarPorCodigo.getValue())
                        cond2 = false;
                }
                if(cond2 == true){
                    p_query = conn.prepareStatement("DELETE FROM mozos WHERE Mo_Cod = ?");
                    p_query.setInt(1, (int) jEliminarPorCodigo.getValue());
                    p_query.executeUpdate();
                    updateForm();
                    updateLista("SELECT Mo_Cod AS \"Código\", Mo_NombreApellido AS \"Nombre\", Mo_Domicilio AS \"Domicilio\", Mo_DNI AS \"DNI\" FROM mozos");
                    label_error.setVisible(true);
                    ocultarCartelExito(label_error);
                }else
                    JOptionPane.showMessageDialog(this, ERROR_MSG_DELETE_TABLE);
            }
            else{
               JOptionPane.showMessageDialog(this, ERROR_MSG_DELETE_EXISTS);
            }
        }catch (SQLException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jbEliminarActionPerformed

    private void jBotonMostrar1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBotonMostrar1ActionPerformed
        String caso;
        caso = jComboConsultas.getSelectedItem().toString();
        try {
            switch(caso){
                case "Mozos":
                    updateLista("SELECT Mo_Cod AS \"Código\", Mo_NombreApellido AS \"Nombre\", Mo_Domicilio AS \"Domicilio\", Mo_DNI AS \"DNI\" FROM mozos ORDER BY Mo_Cod");
                    break;
                case "Platos":
                    updateLista("SELECT P_Cod AS \"Código\", P_Nombre AS \"Nombre\", P_Descripcion AS \"Descripción\", P_Tipo AS \"Tipo\", P_PrecioCosto AS \"Precio de costo\", "  
                                + "P_PrecioVenta AS \"Precio de venta\", P_PrecioPromocion AS \"Precio de promocion\" FROM platos ORDER BY P_Cod");
                    break;
                case "Mozos libres":
                    updateLista("SELECT DISTINCT Mo_Cod AS \"Código\", Mo_NombreApellido AS \"Nombre\" "
                                + "FROM mozos, mesas " 
                                + "WHERE Mo_Cod NOT IN ("
                                + "SELECT Mo_Cod_Atiende "
                                + "FROM mesas)"  
                                + "ORDER by Mo_Cod ");
                    //Sacar mesas del producto
                    break;   
                case "Cantidad de mesas por mozo":
                    updateLista("SELECT Mo_nombreapellido AS \"Nombre\", COUNT (*) AS \"Cantidad\" "
                                + "FROM mozos, mesas " 
                                + "WHERE Mo_Cod = Mo_Cod_Atiende "
                                + "GROUP BY Mo_nombreapellido " 
                                + "ORDER BY Mo_nombreapellido ASC");
                    break;
                case "Platos más consumidos":
                    updateLista("SELECT TABLA_Comidas2.P_Nombre AS \"Nombre\", TABLA_MaximoPorTipo.P_Tipo AS \"Tipo\", TABLA_MaximoPorTipo.max AS \"Veces consumido\""
                                + "FROM ("
                                    + "SELECT P_Tipo, MAX(count) "
                                    + "FROM (SELECT P_Tipo, P_Nombre, COUNT(platos.P_Cod) "
                                        + "FROM platos, se_consume " 
                                        + "WHERE platos.P_Cod = se_consume.P_Cod "
                                        + "GROUP BY P_Tipo, P_Nombre" 
                                    + ") "
                                    + "AS TABLA_Comidas1 "
                                    + "GROUP BY TABLA_Comidas1.P_Tipo"
                                + ") " 
                                + "AS TABLA_MaximoPorTipo, "
                                + "(SELECT P_Tipo, P_Nombre, COUNT(platos.P_Cod) "
                                    + "FROM platos, se_consume "
                                    + "WHERE platos.P_Cod = se_consume.P_Cod " 
                                    + "GROUP BY P_Tipo, P_Nombre "
                                + ") "
                                + "AS TABLA_Comidas2 "
                                + "WHERE TABLA_Comidas2.P_Tipo = TABLA_MaximoPorTipo.P_Tipo "
                                + "AND TABLA_MaximoPorTipo.max = TABLA_Comidas2.count;");
                    break;
                case "Platos nunca consumidos":
                    updateLista("SELECT P_Nombre AS \"Nombre del Plato\", P_Descripcion AS \"Descripcion del Plato\" " 
                                + "FROM platos " 
                                + "WHERE P_Cod NOT IN ( " 
                                + "SELECT P_Cod " 
                                + "FROM Se_Consume ) " 
                                + "GROUP BY P_Cod ");
                    break;
                case "Máximo mínimo y promedio de platos principales":
                    updateLista("SELECT MAX(P_PrecioCosto) AS \"Costo máximo\", MIN(P_PrecioCosto) AS \"Costo mínimo \", AVG(P_PrecioCosto) AS \"Costo promedio\" "
                                + "FROM platos;");
                    break;
            }
        } catch (SQLException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jBotonMostrar1ActionPerformed

    private void jComboConsultasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboConsultasActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboConsultasActionPerformed

    private void jBotonMostrar2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBotonMostrar2ActionPerformed
        String caso;
        caso = jComboConsultasParametros.getSelectedItem().toString();
        try {
            switch(caso){
                case "Mesas asignadas a un mozo":
                    if((int)jParametroConsulta.getValue() == 0)
                        JOptionPane.showMessageDialog(this, ERROR_MSG_CONSULT_EMPTY);
                    else{
                        updateListaConParametros("SELECT Me_Cod AS \"Codigo\", Me_Sector AS \"Sector\", Mo_Cod_Atiende AS \"Mozo asignado\" FROM mesas WHERE Mo_Cod_Atiende = ?", (
                                                 int)jParametroConsulta.getValue());
                        if(jTablaConsultasParametros.getModel().getRowCount() == 0)
                            JOptionPane.showMessageDialog(this, ERROR_MSG_CONSULT_WAITER);
                        updateForm();
                    }
                    break;
                case "Platos consumidos entre dos fechas":
                    jParametroConsulta.setValue(1);
                    updateListaFecha(jPrimeraFecha.getText(), jSegundaFecha.getText());
                    updateForm();
                    break;
                case "Platos consumidos en una mesa":
                    if((int)jParametroConsulta.getValue() == 0)
                        JOptionPane.showMessageDialog(this, ERROR_MSG_CONSULT_EMPTY);
                    else{
                        updateListaConParametros("SELECT P_Cod AS \"Codigo\", P_Nombre AS \"Nombre\", P_Descripcion AS \"Descripcion\""
                                                 + "FROM ("
                                                    + "SELECT platos.P_Cod, P_Nombre, P_Descripcion "
                                                    + "FROM platos, se_consume, consumos " 
                                                    + "WHERE Me_Cod_Realiza = ? " 
                                                    + "AND se_consume.C_Cod = consumos.C_Cod "
                                                    + "AND platos.P_Cod = se_consume.P_Cod"
                                                 + ") " 
                                                 + "AS foo",
                                                 (int)jParametroConsulta.getValue());
                        if(jTablaConsultasParametros.getModel().getRowCount() == 0)
                            JOptionPane.showMessageDialog(this, ERROR_MSG_CONSULT_TABLE);
                        updateForm();
                        //Modificar esta consulta
                    }
                    break;
                case "Cantidad de platos consumidos en una mesa":
                    if((int)jParametroConsulta.getValue() == 0)
                        JOptionPane.showMessageDialog(this, ERROR_MSG_CONSULT_EMPTY);
                    else{
                        updateListaConParametros("SELECT COUNT (*) AS \"Cantidad de platos consumidos en una mesa\""
                                                 + "FROM ("
                                                    + "SELECT platos.P_Cod "
                                                    + "FROM platos, se_consume, consumos " 
                                                    + "WHERE Me_Cod_Realiza = ? " 
                                                    + "AND consumos.C_Cod = se_consume.C_Cod "
                                                    + "AND se_consume.P_Cod = platos.P_Cod"
                                                 + ") " 
                                                 + "AS foo",
                                                 (int)jParametroConsulta.getValue());
                        updateForm();
                    }
                    break;
            }
        } catch (SQLException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jBotonMostrar2ActionPerformed

    private void jComboConsultasParametrosItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboConsultasParametrosItemStateChanged
         if(jComboConsultasParametros.getSelectedItem().toString().equals("Platos consumidos entre dos fechas")){
            jCartelCodigo.setVisible(false);
            jParametroConsulta.setVisible(false);
            jCartelPrimeraFecha.setVisible(true);
            jCartelSegundaFecha.setVisible(true);
            jPrimeraFecha.setVisible(true);
            jSegundaFecha.setVisible(true);
        }else{
            jCartelCodigo.setVisible(true);
            jParametroConsulta.setVisible(true);
            jCartelPrimeraFecha.setVisible(false);
            jCartelSegundaFecha.setVisible(false);
            jPrimeraFecha.setVisible(false);
            jSegundaFecha.setVisible(false);
        }
    }//GEN-LAST:event_jComboConsultasParametrosItemStateChanged

    private void jBotonActualizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBotonActualizarActionPerformed
        try {
            query = conn.createStatement();
            result = query.executeQuery("SELECT COUNT(*) AS \"Cantidad de mozos\""
                                        + "FROM mozos");
            jTablaCantidadMozosDisponibles.setModel(resultToTable(result));
            result = query.executeQuery("SELECT COUNT(*) AS \"Cantidad de mesas\""
                                        + "FROM mesas"); 
            jTablaCantidadMesas.setModel(resultToTable(result));
            result = query.executeQuery("SELECT COUNT(*) AS \"Cantidad de entradas\""
                                        + "FROM platos "
                                        + "WHERE P_Tipo = 'Entrada'"); 
            jTablaCantidadEntradas.setModel(resultToTable(result));
            result = query.executeQuery("SELECT COUNT(*) AS \"Cantidad de platos principales\""
                                        + "FROM platos "
                                        + "WHERE P_Tipo = 'Plato Principal'"); 
            jTablaCantidadPlatosPrincipales.setModel(resultToTable(result));
            result = query.executeQuery("SELECT COUNT(*) AS \"Cantidad de postres\""
                                        + "FROM platos "
                                        + "WHERE P_Tipo = 'Postre'"); 
            jTablaCantidadPostres.setModel(resultToTable(result));
        } catch (SQLException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jBotonActualizarActionPerformed
    
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new MainFrame().setVisible(true);
                } catch (SQLException ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBotonActualizar;
    private javax.swing.JButton jBotonMostrar1;
    private javax.swing.JButton jBotonMostrar2;
    private javax.swing.JLabel jCartelCodigo;
    private javax.swing.JLabel jCartelPrimeraFecha;
    private javax.swing.JLabel jCartelSegundaFecha;
    private javax.swing.JSpinner jCodigo;
    private javax.swing.JComboBox<String> jComboConsultas;
    private javax.swing.JComboBox<String> jComboConsultasParametros;
    private javax.swing.JTextField jDescripcion;
    private javax.swing.JSpinner jEliminarPorCodigo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JTextField jNombre;
    private javax.swing.JPanel jPanelConsultas;
    private javax.swing.JPanel jPanelConsultasParametros;
    private javax.swing.JPanel jPanelEliminar;
    private javax.swing.JPanel jPanelInsertar;
    private javax.swing.JPanel jPanelResumen;
    private javax.swing.JSpinner jParametroConsulta;
    private javax.swing.JTextField jPrecioCosto;
    private javax.swing.JTextField jPrecioPromocion;
    private javax.swing.JTextField jPrecioVenta;
    private javax.swing.JTextField jPrimeraFecha;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JTextField jSegundaFecha;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTablaCantidadEntradas;
    private javax.swing.JTable jTablaCantidadMesas;
    private javax.swing.JTable jTablaCantidadMozosDisponibles;
    private javax.swing.JTable jTablaCantidadPlatosPrincipales;
    private javax.swing.JTable jTablaCantidadPostres;
    private javax.swing.JTable jTablaConsultas;
    private javax.swing.JTable jTablaConsultasParametros;
    private javax.swing.JComboBox<String> jTipo;
    private javax.swing.JButton jbEliminar;
    private javax.swing.JButton jbInsertar;
    private javax.swing.JLabel label_error;
    // End of variables declaration//GEN-END:variables
}
