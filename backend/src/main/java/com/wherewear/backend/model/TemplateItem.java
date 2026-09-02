package com.wherewear.backend.model;

public class TemplateItem {

    private String name;
    private int order;

    public TemplateItem() {
    }

    public TemplateItem(String name, int order) {
        this.name = name;
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
