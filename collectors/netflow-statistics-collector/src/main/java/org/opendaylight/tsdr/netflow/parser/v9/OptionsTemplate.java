/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser.v9;

/**
 * Encapsulates a v9 options template.
 *
 * @author Thomas Pantelis
 */
final class OptionsTemplate {
    private final Template scopeTemplate;
    private final Template optionTemplate;

    private OptionsTemplate(Template scopeTemplate, Template optionTemplate) {
        this.scopeTemplate = scopeTemplate;
        this.optionTemplate = optionTemplate;
    }

    Template getScopeTemplate() {
        return scopeTemplate;
    }

    Template getOptionTemplate() {
        return optionTemplate;
    }

    int getTotalLength() {
        return scopeTemplate.getTotalLength() + optionTemplate.getTotalLength();
    }

    @Override
    public String toString() {
        return "OptionsTemplate [scopeTemplate=" + scopeTemplate + ", optionTemplate=" + optionTemplate + "]";
    }

    static class Builder {
        private final Template.Builder scopeTemplateBuilder = new Template.Builder();
        private final Template.Builder optionTemplateBuilder = new Template.Builder();

        void addScopeField(int type, int length) {
            scopeTemplateBuilder.addField(type, length);
        }

        void addOptionField(int type, int length) {
            optionTemplateBuilder.addField(type, length);
        }

        OptionsTemplate build() {
            return new OptionsTemplate(scopeTemplateBuilder.build(), optionTemplateBuilder.build());
        }
    }
}
