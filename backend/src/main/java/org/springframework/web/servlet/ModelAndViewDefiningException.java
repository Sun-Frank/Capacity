package org.springframework.web.servlet;

import javax.servlet.ServletException;
import org.springframework.util.Assert;

@Deprecated
public class ModelAndViewDefiningException extends ServletException {

    private static final long serialVersionUID = 1L;

    private final ModelAndView modelAndView;

    public ModelAndViewDefiningException(ModelAndView modelAndView) {
        Assert.notNull(modelAndView, "ModelAndView must not be null in ModelAndViewDefiningException");
        this.modelAndView = modelAndView;
    }

    public ModelAndView getModelAndView() {
        return this.modelAndView;
    }
}
