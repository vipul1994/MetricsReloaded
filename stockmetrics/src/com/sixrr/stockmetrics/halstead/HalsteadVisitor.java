/*
 * Copyright 2005-2016 Sixth and Red River Software, Bas Leijdekkers
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.sixrr.stockmetrics.halstead;

import com.intellij.psi.*;
import com.sixrr.metrics.utils.MethodUtils;
import com.sixrr.stockmetrics.utils.ExpressionUtils;

import java.util.HashSet;
import java.util.Set;

public class HalsteadVisitor extends JavaRecursiveElementVisitor {
    private int numOperands = 0;
    private int numOperators = 0;
    private final Set<String> operators = new HashSet<String>(32);
    private final Set<String> operands = new HashSet<String>(32);
    private boolean inCompileTimeConstant = false;

    @Override
    public void visitReferenceExpression(PsiReferenceExpression expression) {
        super.visitReferenceExpression(expression);

        final PsiElement element = expression.resolve();
        if (element != null && element instanceof PsiVariable) {
            final String expressionText = expression.getText();
            registerOperand(expressionText);
        }
    }

    private int getNumDistinctOperands() {
        return operands.size();
    }

    private int getNumDistinctOperators() {
        return operators.size();
    }

    public int getLength() {
        return numOperands + numOperators; // N1 + N2
    }

    public double getEstimatedLength() {
        return operands.size() * Math.log(operands.size()) + operators.size() * Math.log(operators.size());
    }

    public double getIntelligence() {
        // this.INTELLIGENT_CONTENT=(2*this.n2/(this.n1*this.N2)) * this.VOLUME;
        if (getNumDistinctOperands() != 0 && numOperands != 0) {
            return (2 * getNumDistinctOperands() / (getNumDistinctOperators() * numOperands)) * getVolume();
        } else {
            return 0.0;
        }
    }

    public double getProgramTime() {
        return getEffort() / 18.0;
    }

    public double getn1() {
        return operators.size();
    }

    public double getn2() {
        return operands.size();
    }

    public double getN1() {
        return numOperators;
    }

    public double getN2() {
        return numOperands;
    }

    public int getVocabulary() {
        return operands.size() + operators.size();
    }

    public double getDifficulty() {
        final int N2 = numOperands;
        final int n1 = getNumDistinctOperators();
        final int n2 = getNumDistinctOperands();
        return n2 == 0 ? 0.0 : ((double) n1 / 2.0) * ((double) N2 / (double) n2);
    }

    public double getVolume() {
        final double vocabulary = (double) getVocabulary();
        return (double) getLength() * Math.log(vocabulary) / Math.log(2.0);
    }

    public double getEffort() {
        return getDifficulty() * getVolume();
    }

    public double getBugs() {
        final double effort = getEffort();
        return Math.pow(effort, 2.0 / 3.0) / 3000.0;
    }

    @Override
    public void visitLiteralExpression(PsiLiteralExpression expression) {
        if (inCompileTimeConstant) {
            return;
        }
        if (ExpressionUtils.isEvaluatedAtCompileTime(expression)) {
            inCompileTimeConstant = true;
        }
        super.visitLiteralExpression(expression);
        final String text = expression.getText();
        registerOperand(text);
        inCompileTimeConstant = false;
    }

    @Override
    public void visitBinaryExpression(PsiBinaryExpression expression) {
        if (inCompileTimeConstant) {
            return;
        }
        if (ExpressionUtils.isEvaluatedAtCompileTime(expression)) {
            inCompileTimeConstant = true;
            final String text = expression.getText();
            registerOperand(text);
        }
        super.visitBinaryExpression(expression);
        final PsiJavaToken sign = expression.getOperationSign();
        registerSign(sign);
        inCompileTimeConstant = false;
    }

    @Override
    public void visitPrefixExpression(PsiPrefixExpression expression) {
        if (inCompileTimeConstant) {
            return;
        }
        if (ExpressionUtils.isEvaluatedAtCompileTime(expression)) {
            inCompileTimeConstant = true;
            final String text = expression.getText();
            registerOperand(text);
        }
        super.visitPrefixExpression(expression);
        final PsiJavaToken sign = expression.getOperationSign();

        registerSign(sign);
        inCompileTimeConstant = false;
    }

    @Override
    public void visitPostfixExpression(PsiPostfixExpression expression) {
        if (inCompileTimeConstant) {
            return;
        }
        if (ExpressionUtils.isEvaluatedAtCompileTime(expression)) {
            inCompileTimeConstant = true;
            final String text = expression.getText();
            registerOperand(text);
        }
        super.visitPostfixExpression(expression);
        final PsiJavaToken sign = expression.getOperationSign();
        registerSign(sign);
        inCompileTimeConstant = false;
    }

    @Override
    public void visitKeyword(PsiKeyword psiKeyword) {
        super.visitKeyword(psiKeyword);
        registerSign(psiKeyword);
    }

    @Override
    public void visitMethodCallExpression(PsiMethodCallExpression callExpression) {
        super.visitMethodCallExpression(callExpression);
        final PsiMethod method = callExpression.resolveMethod();
        if (method != null) {
            final String signature = MethodUtils.calculateSignature(method);
            registerOperator(signature);
        }
    }

    private void registerSign(PsiJavaToken sign) {
        final String text = sign.getText();
        registerOperator(text);
    }

    private void registerOperator(String operator) {
        numOperators++;
        operators.add(operator);
    }

    private void registerOperand(String operand) {
        numOperands++;
        operands.add(operand);
    }
}
