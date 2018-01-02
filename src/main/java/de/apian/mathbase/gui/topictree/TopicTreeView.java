/*
 * Copyright (c) 2017 MathBox P-Seminar 16/18. All rights reserved.
 * This product is licensed under the GNU General Public License v3.0.
 * See LICENSE file for further information.
 */

package de.apian.mathbase.gui.topictree;

import de.apian.mathbase.gui.ContentPane;
import de.apian.mathbase.gui.MainPane;
import de.apian.mathbase.gui.dialog.AddTopicDialog;
import de.apian.mathbase.gui.dialog.PasswordDialog;
import de.apian.mathbase.gui.dialog.RenameTopicDialog;
import de.apian.mathbase.util.Images;
import de.apian.mathbase.xml.NodeNotFoundException;
import de.apian.mathbase.xml.TitleCollisionException;
import de.apian.mathbase.xml.TopicTreeController;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.util.Optional;

/**
 * GUI-Repräsentation des Themenbaums.
 *
 * @author Nikolas Kirschstein
 * @version 1.0
 * @since 1.0
 */
public class TopicTreeView extends TreeView<String> {

    /**
     * Basisanzeigefläche
     *
     * @since 1.0
     */
    private MainPane mainPane;

    /**
     * Themenbaumkontrolleur
     *
     * @since 1.0
     */
    private TopicTreeController topicTreeController;

    /**
     * Konstruktion eines Themenbaums
     *
     * @param mainPane            Basisanzeigefläche
     * @param topicTreeController Themenbaumkontrolleur
     * @since 1.0
     */
    public TopicTreeView(MainPane mainPane, TopicTreeController topicTreeController) {
        this.topicTreeController = topicTreeController;
        this.mainPane = mainPane;

        setRoot(new TreeItem<>());
        setShowRoot(false);
        setEditable(true);

        initSelectionBehavior();
        initContextMenu();

        setCellFactory(param -> new DraggableTreeCell(mainPane, topicTreeController));
    }

    /**
     * Füllen des Themenbaums
     *
     * @since 1.0
     */
    public void build() {
        build(getRoot());
    }

    /**
     * Rekursives Füllen ab der Wurzel
     *
     * @param parent Jeweiliger Elternknoten im Themenbaum
     * @since 1.0
     */
    private void build(TreeItem<String> parent) {
        for (String s : topicTreeController.getChildren(parent.getValue())) {
            TreeItem<String> child = new TreeItem<>(s);
            parent.getChildren().add(child);
            build(child);
        }
    }

    /**
     * Konstruktion eines gefilterten Themenbaums
     *
     * @param key Suchbegriff
     * @return gefilterter Themenbaum
     * @since 1.0
     */
    public TreeView<String> filter(String key) {
        TopicTreeView treeView = new TopicTreeView(mainPane, topicTreeController);
        filter(getRoot(), treeView.getRoot(), key);
        return treeView;
    }

    /**
     * Rekursive Konstruktion eines gefilterten Baums ab der Wurzel
     *
     * @param originalParent Elternknoten im originalen Baum
     * @param filteredParent Elternknoten im gefilterten Baum
     * @param key            Suchbegriff
     * @since 1.0
     */
    private void filter(TreeItem<String> originalParent, TreeItem<String> filteredParent, String key) {
        for (TreeItem<String> originalChild : originalParent.getChildren()) {
            if (subtreeContainsIgnoreCase(originalChild, key)) {
                TreeItem<String> filteredChild = new TreeItem<>(originalChild.getValue());
                filteredChild.setExpanded(true);
                filteredParent.getChildren().add(filteredChild);
                filter(originalChild, filteredChild, key);
            }
        }
    }

    /**
     * Prüfung eines Teilbaums auf enthalten eines Suchbegriffes
     *
     * @param parent Elternknoten
     * @param key    Suchbegriff
     * @return Boolesche Aussage, ob der Suchbegriff enthalten ist
     * @since 1.0
     */
    private boolean subtreeContainsIgnoreCase(TreeItem<String> parent, String key) {
        if (parent.getValue().toLowerCase().contains(key.toLowerCase()))
            return true;
        for (TreeItem<String> child : parent.getChildren())
            if (child.getValue().toLowerCase().contains(key.toLowerCase()) || subtreeContainsIgnoreCase(child, key))
                return true;
        return false;
    }

    /**
     * Initialisierung des Auswahlverhaltens
     *
     * @since 1.0
     */
    private void initSelectionBehavior() {
        getSelectionModel().select(null);
        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            String title = newValue != null ? newValue.getValue() : null;
            ContentPane contentPane = new ContentPane(title, mainPane, topicTreeController);
            mainPane.getItems().set(1, contentPane);
        });

        // Wenn auf leeren Platz im Themenbaum geclickt wird, soll nichts ausgewählt sein
        addEventHandler(MouseEvent.MOUSE_CLICKED, a -> {
            Node node = a.getPickResult().getIntersectedNode();
            if (node instanceof TreeCell && ((TreeCell) node).getText() == null)
                getSelectionModel().select(null);
        });
    }

    /**
     * Initiierung eines nigelnagelschnieken Kontextmenüs (mit Senf)
     *
     * @since 1.0
     */
    private void initContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem addItem = new MenuItem("Hinzufügen");
        addItem.setGraphic(new ImageView(Images.getInternal("add.png")));
        addItem.setOnAction(a -> addUnderSelected());

        MenuItem renameItem = new MenuItem("Umbenennen");
        renameItem.setGraphic(new ImageView(Images.getInternal("rename.png")));
        renameItem.setOnAction(a -> renameSelected());

        MenuItem removeItem = new MenuItem("Entfernen");
        removeItem.setGraphic(new ImageView(Images.getInternal("remove.png")));
        removeItem.setOnAction(a -> removeSelected());

        MenuItem expandItem = new MenuItem("Alle ausklappen");
        expandItem.setGraphic(new ImageView(Images.getInternal("expand.png")));
        expandItem.setOnAction(a -> setExpandedAll(getRoot(), true));

        MenuItem collapseItem = new MenuItem("Alle einklappen");
        collapseItem.setGraphic(new ImageView(Images.getInternal("collapse.png")));
        collapseItem.setOnAction(a -> setExpandedAll(getRoot(), false));

        contextMenu.getItems().addAll(addItem, new SeparatorMenuItem(), renameItem, removeItem,
                new SeparatorMenuItem(), expandItem, collapseItem);
        setContextMenu(contextMenu);
    }

    /**
     * Festlegung der Aktionen beim Hinzufügen eines Themas
     *
     * @return Implementierung des funktionalen Interfaces {@code EventHandler}
     * @see <a href="https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html">Lambda-Ausdrücke</a>
     * @since 1.0
     */
    private void addUnderSelected() {
        AddTopicDialog dialog = new AddTopicDialog(mainPane, topicTreeController);
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(title -> {
            TreeItem<String> selectedItem = getSelectionModel().getSelectedItem();
            if (selectedItem == null)
                selectedItem = getRoot();

            try {
                topicTreeController.addNode(title, selectedItem.getValue());
                TreeItem<String> newItem = new TreeItem<>(title);
                selectedItem.getChildren().add(newItem);
                selectedItem.setExpanded(true);
            } catch (NodeNotFoundException | IOException | TitleCollisionException e) {
                e.printStackTrace();
            }
        });
    }

    private void renameSelected() {
        TreeItem<String> selectedItem = getSelectionModel().getSelectedItem();
        if (selectedItem == null)
            return;

        RenameTopicDialog dialog = new RenameTopicDialog(mainPane, topicTreeController);
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(title -> {
            try {
                topicTreeController.renameNode(selectedItem.getValue(), title);
                selectedItem.setValue(title);
            } catch (NodeNotFoundException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Festlegung der Aktionen beim Löschen eines Themas
     *
     * @return Implementierung des funktionalen Interfaces {@code EventHandler}
     * @see <a href="https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html">Lambda-Ausdrücke</a>
     * @since 1.0
     */
    private void removeSelected() {
        TreeItem<String> selectedItem = getSelectionModel().getSelectedItem();
        if (selectedItem == null)
            return;

        PasswordDialog dialog = new PasswordDialog(mainPane);
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(pw -> {
            try {
                selectedItem.getParent().getChildren().remove(selectedItem);
                topicTreeController.removeNode(selectedItem.getValue());
                getSelectionModel().select(null);
            } catch (NodeNotFoundException | IOException e) {
                e.printStackTrace();
            }
        });

    }

    private void setExpandedAll(TreeItem<String> parent, boolean expanded) {
        for (TreeItem<String> child : parent.getChildren()) {
            child.setExpanded(expanded);
            setExpandedAll(child, expanded);
        }
    }
}
