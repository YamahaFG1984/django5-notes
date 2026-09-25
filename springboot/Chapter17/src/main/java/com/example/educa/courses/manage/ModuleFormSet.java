package com.example.educa.courses.manage;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.example.educa.courses.Module;

/**
 * 模块“表单集合”，≈ inlineformset_factory(Course, Module, fields=['title', 'description'], extra=2, can_delete=True)。
 * Django 的 formset 靠 management_form（TOTAL_FORMS 等隐藏字段）知道有几个子表单；
 * Spring 直接把 rows[0].title、rows[1].title…… 绑定成一个 List，不需要额外的管理字段。
 */
public class ModuleFormSet {

    public static final int EXTRA = 2;

    private List<@Valid Row> rows = new ArrayList<>();

    public static class Row {

        /** 已有模块的 id；新增的行为空 */
        private Long id;

        @Size(max = 200)
        private String title = "";

        private String description = "";

        /** can_delete=True：每行一个“删除”复选框 */
        private boolean delete;

        public Row() {
        }

        public Row(Long id, String title, String description) {
            this.id = id;
            this.title = title;
            this.description = description;
        }

        /** extra 行什么都没填：忽略（Django formset 对“未改动的空表单”也是这样处理的） */
        public boolean isEmptyExtra() {
            return id == null && (title == null || title.isBlank()) && (description == null || description.isBlank());
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title == null ? "" : title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description == null ? "" : description;
        }

        public boolean isDelete() {
            return delete;
        }

        public void setDelete(boolean delete) {
            this.delete = delete;
        }
    }

    public static ModuleFormSet of(List<Module> modules) {
        ModuleFormSet formSet = new ModuleFormSet();
        modules.forEach(m -> formSet.rows.add(new Row(m.getId(), m.getTitle(), m.getDescription())));
        for (int i = 0; i < EXTRA; i++) {
            formSet.rows.add(new Row());
        }
        return formSet;
    }

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }
}
