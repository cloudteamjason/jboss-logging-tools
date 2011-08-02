/*
 *  JBoss, Home of Professional Open Source Copyright 2011, Red Hat, Inc., and
 *  individual contributors by the @authors tag. See the copyright.txt in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it under the
 *  terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 2.1 of the License, or (at your option)
 *  any later version.
 *
 *  This software is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this software; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 *  site: http://www.fsf.org.
 */
package org.jboss.logging.generator;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.logging.generator.LoggingTools.annotations;
import static org.jboss.logging.generator.util.ElementHelper.isAnnotatedWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a> - 20.Feb.2011
 */
final class MethodParameterFactory {

    /**
     * Private constructor for factory.
     */
    private MethodParameterFactory() {
    }

    public static Set<MethodParameter> of(final Elements elements, final Types types, final ExecutableElement method) {
        final Set<MethodParameter> result = new LinkedHashSet<MethodParameter>();
        final List<? extends VariableElement> params = method.getParameters();
        int index = 0;
        for (VariableElement param : params) {
            String formatClass = null;
            // Format class may not yet be compiled, so get it in a roundabout way
            for (AnnotationMirror mirror : param.getAnnotationMirrors()) {
                final DeclaredType annotationType = mirror.getAnnotationType();
                if (annotationType.equals(types.getDeclaredType(elements.getTypeElement(annotations().formatWith().getName())))) {
                    final AnnotationValue value = mirror.getElementValues().values().iterator().next();
                    formatClass = ((TypeElement) (((DeclaredType) value.getValue()).asElement())).getQualifiedName().toString();
                }
            }
            final String qualifiedType;
            if (param.asType().getKind().isPrimitive()) {
                qualifiedType = param.asType().toString();
            } else {
                switch ((param.asType().getKind())) {
                    case ARRAY:
                        qualifiedType = param.asType().toString().replace("[]", "");
                        break;
                    default:
                        qualifiedType = types.asElement(param.asType()).toString();
                        break;
                }
            }
            if (method.isVarArgs()) {
                result.add(new MethodParameterImpl(qualifiedType, param, formatClass, (++index == params.size())));
            } else {
                result.add(new MethodParameterImpl(qualifiedType, param, formatClass, false));
            }
        }
        return result;
    }

    public static MethodParameter forMessageMethod(final MessageMethod messageMethod) {
        return new MethodParameter() {
            @Override
            public boolean isCause() {
                return false;
            }

            @Override
            public boolean isMessage() {
                return true;
            }

            @Override
            public boolean isParam() {
                return false;
            }

            @Override
            public String getFormatterClass() {
                return null;
            }

            @Override
            public Class<?> paramClass() {
                return null;
            }

            @Override
            public Object getRawType() {
                return null;
            }

            @Override
            public String type() {
                return String.class.getName();
            }

            @Override
            public String name() {
                return messageMethod.messageMethodName();
            }

            @Override
            public boolean isArray() {
                return false;
            }

            @Override
            public boolean isPrimitive() {
                return false;
            }

            @Override
            public boolean isVarArgs() {
                return false;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + (type() == null ? 0 : type().hashCode());
                result = prime * result + (name() == null ? 0 : name().hashCode());
                return result;
            }

            @Override
            public boolean equals(final Object obj) {
                if (obj == this) {
                    return true;
                }
                if (!(obj instanceof MethodParameterImpl)) {
                    return false;
                }
                final MethodParameterImpl other = (MethodParameterImpl) obj;
                if ((this.type() == null) ? (other.type() != null) : !type().equals(other.type())) {
                    return false;
                }
                return !((this.name() == null) ? (other.name() != null) : !name().equals(other.name()));
            }

            @Override
            public int compareTo(final Parameter other) {
                int result = this.type().compareTo(other.type());
                result = (result != 0) ? result : this.name().compareTo(other.name());
                return result;
            }
        };
    }

    private static class MethodParameterImpl implements MethodParameter {

        private final VariableElement param;
        private final String qualifiedType;
        private final String formatterClass;
        private final Class<?> paramClass;
        private final boolean isParam;
        private final boolean isVarArgs;

        /**
         * Only allow construction from within the parent class.
         *
         * @param qualifiedType  the qualified type name of the parameter.
         * @param param          the parameter.
         * @param formatterClass the formatter class, or {@code null} if none
         */
        MethodParameterImpl(final String qualifiedType, final VariableElement param, final String formatterClass, final boolean isVarArgs) {
            this.qualifiedType = qualifiedType;
            this.param = param;
            this.formatterClass = formatterClass;
            if (isAnnotatedWith(param, annotations().param())) {
                paramClass = Object.class;
                isParam = true;
            } else {
                isParam = false;
                paramClass = null;
            }
            this.isVarArgs = isVarArgs;
        }

        @Override
        public boolean isCause() {
            return isAnnotatedWith(param, annotations().cause());
        }

        @Override
        public boolean isMessage() {
            return false;
        }

        @Override
        public boolean isParam() {
            return isParam;
        }

        @Override
        public String type() {
            return qualifiedType;
        }

        @Override
        public String getFormatterClass() {
            return formatterClass;
        }

        @Override
        public String name() {
            return param.getSimpleName().toString();
        }

        @Override
        public boolean isArray() {
            return param.asType().getKind() == TypeKind.ARRAY;
        }

        @Override
        public boolean isPrimitive() {
            return param.asType().getKind().isPrimitive();
        }

        @Override
        public boolean isVarArgs() {
            return isVarArgs;
        }

        @Override
        public Class<?> paramClass() {
            return paramClass;
        }

        @Override
        public VariableElement getRawType() {
            return param;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int hash = 1;
            hash = prime * hash + ((qualifiedType == null) ? 0 : qualifiedType.hashCode());
            hash = prime * hash + ((param == null) ? 0 : param.hashCode());
            return hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof MethodParameterImpl)) {
                return false;
            }
            final MethodParameterImpl other = (MethodParameterImpl) obj;
            if ((this.param == null) ? (other.param != null) : !this.param.equals(other.param)) {
                return false;
            }
            if ((this.qualifiedType == null) ? (other.qualifiedType != null) : !this.qualifiedType.equals(other.qualifiedType)) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(final Parameter other) {
            int result = this.type().compareTo(other.type());
            result = (result != 0) ? result : this.name().compareTo(other.name());
            return result;
        }
    }
}