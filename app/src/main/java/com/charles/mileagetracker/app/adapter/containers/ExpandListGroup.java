package com.charles.mileagetracker.app.adapter.containers;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by charles on 7/9/14.
 */
public class ExpandListGroup {
    private String name;
    private ArrayList<ExpandListChild> items;
    private int group_id = -1;

    public ExpandListGroup(int group_id) {
        this.group_id = group_id;
        items = new ArrayList<ExpandListChild>();
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

    public ArrayList<ExpandListChild> getItems() {
        return items;
    }

    public void addItem(ExpandListChild item) {
        items.add(item);
    }


    public void reverseChildren() {
        Collections.reverse(items);
    }
}
