package org.hvdw.jexiftoolgui.view;

import ch.qos.logback.classic.Logger;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.hvdw.jexiftoolgui.MyVariables;
import org.hvdw.jexiftoolgui.ProgramTexts;
import org.hvdw.jexiftoolgui.controllers.SQLiteJDBC;
import org.hvdw.jexiftoolgui.facades.SystemPropertyFacade;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.hvdw.jexiftoolgui.facades.SystemPropertyFacade.SystemPropertyKey.LINE_SEPARATOR;

public class AddFavorite extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField favoritenametextField;
    private JLabel copiedcommandquerylabel;
    private JScrollPane scrollPane;
    private JTable favoritestable;
    private JLabel lblCommandQuery;
    private JLabel commandqueryTopText;

    private JPanel jp = null;
    private String favtype = "";
    private String chosenName = "";
    private String favtypeText = "";
    private String cmd_qry = "";

    private final String commandTxt = "<html>" + ResourceBundle.getBundle("translations/program_strings").getString("fav.commandtext") + "<br><br></html>";
    private final String queryTxt = "<html>" + ResourceBundle.getBundle("translations/program_strings").getString("fav.querytext") + "<br><br></html>";

    private final static Logger logger = (Logger) LoggerFactory.getLogger(AddFavorite.class);


    public AddFavorite() {
        setContentPane(contentPane);
        Locale currentLocale = new Locale.Builder().setLocale(MyVariables.getCurrentLocale()).build();
        contentPane.applyComponentOrientation(ComponentOrientation.getOrientation(currentLocale));
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        favoritestable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                DefaultTableModel model = (DefaultTableModel) favoritestable.getModel();
                int selectedRowIndex = favoritestable.getSelectedRow();
                favoritenametextField.setText(model.getValueAt(selectedRowIndex, 0).toString());
                chosenName = model.getValueAt(selectedRowIndex, 0).toString();
                copiedcommandquerylabel.setText(model.getValueAt(selectedRowIndex, 1).toString());
            }
        });
    }

    private String loadfavorites(String favoriteType) {
        String sql = "select favorite_name, command_query from userFavorites where favorite_type='" + favoriteType + "' order by favorite_Name";
        String favorites = SQLiteJDBC.generalQuery(sql, "disk");
        //lblfavorites.setText(String.format(ProgramTexts.HTML, 300, favorites.replace("\n", "<br>")));
        return favorites;

    }

    private void displayfavorites(String favorites, String favoriteType) {

        DefaultTableModel model = (DefaultTableModel) favoritestable.getModel();
        model.setColumnIdentifiers(new String[]{ResourceBundle.getBundle("translations/program_strings").getString("fav.name"), favoriteType});
        //favoritestable.setModel(model);
        favoritestable.getColumnModel().getColumn(0).setPreferredWidth(120);
        favoritestable.getColumnModel().getColumn(1).setPreferredWidth(350);
        model.setRowCount(0);

        Object[] row = new Object[1];

        if (favorites.length() > 0) {
            String[] lines = favorites.split(SystemPropertyFacade.getPropertyByKey(LINE_SEPARATOR));

            for (String line : lines) {
                //String[] cells = lines[i].split(":", 1); // Only split on first : as some tags also contain (multiple) :
                String[] cells = line.split("\\t");
                model.addRow(new Object[]{cells[0], cells[1]});
                //model.addRow(new Object[]{line});
            }
        }

    }

    private void SaveFavorite() {
        String sql = "";
        String queryresult = "";
        String chosenname = favoritenametextField.getText().trim();

        // We do save to the database using single quotes, so if the command or the query contains single quotes we need to escape them
        // by doubling them (as that is what databases expect)
        // We can not do that at the start as the user would see those escaped single quotes in his/her command
        String cmd_qry_escaped = cmd_qry.replace("'", "''");
        logger.debug("cmd_qry_escaped: " + cmd_qry_escaped);
        if (!"".equals(chosenname)) { // user gave a favorites name
            // Check if already exists
            sql = "select favorite_name from userFavorites where favorite_name='" + chosenname + "' and favorite_type='" + favtype + "';";
            queryresult = SQLiteJDBC.singleFieldQuery(sql, "favorite_name");
            if (!"".equals(queryresult)) { // so we have already this name and we want it updated
                int result = JOptionPane.showConfirmDialog(jp, ResourceBundle.getBundle("translations/program_strings").getString("fav.overwrite") + chosenname + "\"?",
                        ResourceBundle.getBundle("translations/program_strings").getString("fav.overwriteshort"), JOptionPane.OK_CANCEL_OPTION);
                if (result == 0) { //OK
                    // user wants us to overwrite
                    logger.info("user wants to update the favorites with name: " + chosenname);
                    sql = "update userFavorites set favorite_type='" + favtype + "',"
                            + " favorite_name='" + chosenname + "',"
                            + " command_query='" + cmd_qry_escaped + "'"
                            + " where favorite_name='" + chosenname + "'"
                            + " and favorite_type='" + favtype + "'";
                    logger.debug("update sql:" + sql);
                    queryresult = SQLiteJDBC.insertUpdateQuery(sql, "disk");
                    if (!"".equals(queryresult)) { //means we have an error
                        JOptionPane.showMessageDialog(jp, ResourceBundle.getBundle("translations/program_strings").getString("fav.updateerror") + chosenname, ResourceBundle.getBundle("translations/program_strings").getString("fav.updateerrshort"), JOptionPane.ERROR_MESSAGE);
                    } else { //success
                        JOptionPane.showMessageDialog(jp, ResourceBundle.getBundle("translations/program_strings").getString("fav.saved") + " " + chosenname, ResourceBundle.getBundle("translations/program_strings").getString("fav.savedshort"), JOptionPane.INFORMATION_MESSAGE);
                    }
                } // result 2 means cancel; do nothing
            } else { // No name from DB, so a new favorite record
                logger.debug("insert new favorite named: " + chosenname);
                sql = "insert into userFavorites(favorite_type, favorite_name, command_query) "
                        + " values('"
                        + favtype + "','"
                        + chosenname + "','"
                        + cmd_qry_escaped + "')";
                logger.info("insert sql: " + sql);
                queryresult = SQLiteJDBC.insertUpdateQuery(sql, "disk");
                if (!"".equals(queryresult)) { //means we have an error
                    JOptionPane.showMessageDialog(jp, ResourceBundle.getBundle("translations/program_strings").getString("fav.inserterror") + " " + chosenname, ResourceBundle.getBundle("translations/program_strings").getString("fav.inserterrshort"), JOptionPane.ERROR_MESSAGE);
                } else { //success
                    JOptionPane.showMessageDialog(jp, ResourceBundle.getBundle("translations/program_strings").getString("fav.saved") + "" + chosenname, ResourceBundle.getBundle("translations/program_strings").getString("fav.savedshort"), JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } else { // user did not provide a lensname to insert/update
            JOptionPane.showMessageDialog(jp, ResourceBundle.getBundle("translations/program_strings").getString("fav.nofavname"), ResourceBundle.getBundle("translations/program_strings").getString("fav.nofavnameshort"), JOptionPane.ERROR_MESSAGE);
        }

        //return queryresult;
    }

    private void onOK() {
        // add your code here
        //chosenName = favoritenametextField.getText();
        SaveFavorite();
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public void showDialog(JPanel rootpanel, String favoriteType, String command_query) {
        pack();
        //setLocationRelativeTo(null);
        setLocationByPlatform(true);
        favoritenametextField.setText("");
        favtypeText = favoriteType.replace("_", " ");

        // Make table readonly
        favoritestable.setDefaultEditor(Object.class, null);

        jp = rootpanel; // Need to save the rootpanel for the onOK method
        favtype = favoriteType; // for onOK method
        cmd_qry = command_query; // for onOK method

        if (favtypeText.contains("Command")) {
            setTitle(ResourceBundle.getBundle("translations/program_strings").getString("favoriteaddcommand.title"));
        } else {
            setTitle(ResourceBundle.getBundle("translations/program_strings").getString("favoriteaddquery.title"));
        }
        setTitle("Add " + favtypeText);
        lblCommandQuery.setText(favtypeText + ": ");
        if ("Exiftool_Command".equals(favoriteType)) {
            commandqueryTopText.setText(commandTxt);
        } else {
            commandqueryTopText.setText(queryTxt);
        }
        copiedcommandquerylabel.setText(String.format(ProgramTexts.HTML, 400, command_query));
        // Get current defined favorites
        String favorites = loadfavorites(favoriteType);
        logger.info("retrieved favorites: " + favorites);
        displayfavorites(favorites, favoriteType);

        setVisible(true);

        //return chosenName;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(15, 15, 15, 15), -1, -1));
        contentPane.setPreferredSize(new Dimension(700, 300));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        this.$$$loadButtonText$$$(buttonOK, this.$$$getMessageFromBundle$$$("translations/program_strings", "dlg.ok"));
        panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        this.$$$loadButtonText$$$(buttonCancel, this.$$$getMessageFromBundle$$$("translations/program_strings", "dlg.cancel"));
        panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), 8, 5));
        panel3.add(panel4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 1, false));
        final JLabel label1 = new JLabel();
        label1.setText("Name:");
        panel4.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        favoritenametextField = new JTextField();
        panel4.add(favoritenametextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(450, -1), null, 0, false));
        lblCommandQuery = new JLabel();
        lblCommandQuery.setText("Command:");
        panel4.add(lblCommandQuery, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        copiedcommandquerylabel = new JLabel();
        copiedcommandquerylabel.setText("command or query");
        panel4.add(copiedcommandquerylabel, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(450, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Current defined favorite(s):");
        panel4.add(label2, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scrollPane = new JScrollPane();
        panel4.add(scrollPane, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        favoritestable = new JTable();
        favoritestable.setPreferredScrollableViewportSize(new Dimension(600, 400));
        scrollPane.setViewportView(favoritestable);
        final Spacer spacer2 = new Spacer();
        panel4.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        commandqueryTopText = new JLabel();
        commandqueryTopText.setText("command or query text");
        panel3.add(commandqueryTopText, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(450, -1), null, 0, false));
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
