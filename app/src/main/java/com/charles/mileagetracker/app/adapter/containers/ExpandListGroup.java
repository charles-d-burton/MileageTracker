package com.charles.mileagetracker.app.adapter.containers;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Created by charles on 7/9/14.
 */
public class ExpandListGroup implements Serializable {
    private String name;
    private ArrayList<ExpandListChild> listChildren;
    private int group_id = -1;

    public ExpandListGroup(int group_id) {
        this.group_id = group_id;
        listChildren = new ArrayList<ExpandListChild>();
    }


    public String getName() {
        return name;
    }

    public int getGroupId() {
        return group_id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<ExpandListChild> getListChildren() {
        return listChildren;
    }

    public void addItem(ExpandListChild item) {
        listChildren.add(item);
        item.setExpandListGroup(this);
    }


    public void reverseChildren() {

        Collections.reverse(listChildren);
    }

    public int getChildCount() {
        return listChildren.size();
    }
}
