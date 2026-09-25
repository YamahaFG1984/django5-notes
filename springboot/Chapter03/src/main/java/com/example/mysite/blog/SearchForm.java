package com.example.mysite.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** class SearchForm(forms.Form): query = forms.CharField() */
public class SearchForm {

    @NotBlank
    @Size(max = 200)
    private String query;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
