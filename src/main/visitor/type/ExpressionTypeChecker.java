package main.visitor.type;

import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.types.NoType;
import main.ast.types.Type;
import main.ast.types.primitives.BoolType;
import main.ast.types.primitives.IntType;
import main.compileError.CompileError;
import main.compileError.typeError.UnsupportedOperandType;
import main.visitor.Visitor;

import javax.naming.BinaryRefAddr;
import javax.xml.stream.events.NotationDeclaration;

public class ExpressionTypeChecker extends Visitor<Type> {

    @Override
    public Type visit(BinaryExpression binaryExpression) {
        Expression lValue, rValue;
        BinaryOperator operation;

        lValue = binaryExpression.getFirstOperand();
        rValue = binaryExpression.getSecondOperand();
        operation = binaryExpression.getBinaryOperator();

        Type typeLValue, typeRValue;
        typeLValue = lValue.accept(this);
        typeRValue = rValue.accept(this);

        //add, sub, mult, div
        if(
                operation == BinaryOperator.add
                ||
                operation == BinaryOperator.sub
                ||
                operation == BinaryOperator.mult
                ||
                operation == BinaryOperator.div
        ) {
            if(typeLValue instanceof IntType && typeRValue instanceof IntType) { // int * int -> int
                return new IntType();
            }
            if(typeLValue instanceof NoType && typeRValue instanceof NoType) { // noType * noType -> noType
                return new NoType();
            }
            if(typeLValue instanceof NoType && typeRValue instanceof IntType) { // noType * int -> int
                return new IntType();
            }
            if(typeLValue instanceof IntType && typeRValue instanceof NoType) { // int * noType -> int
                return new IntType();
            }

            //Error
            binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), operation.name()));
        }

        //eq
        if(
                operation == BinaryOperator.eq
        ) {
            if(typeLValue instanceof IntType && typeRValue instanceof IntType) { //int * int -> bool
                return new BoolType();
            }
            if(typeLValue instanceof BoolType && typeRValue instanceof BoolType) { //bool * bool -> bool
                return new BoolType();
            }
            if(typeLValue instanceof NoType && typeRValue instanceof NoType) { //noType * noType -> noType
                return new NoType();
            }
            if(typeLValue instanceof NoType && typeRValue instanceof IntType) { //noType * int -> int
                return new IntType();
            }
            if(typeLValue instanceof IntType && typeRValue instanceof NoType) { //int * noType -> int
                return new IntType();
            }
            if(typeLValue instanceof NoType && typeRValue instanceof BoolType) { //noType * bool -> bool
                return new BoolType();
            }
            if(typeLValue instanceof BoolType && typeRValue instanceof NoType) { //bool * noType -> bool
                return new BoolType();
            }

            //Error
            binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), operation.name()));
        }

        //gt, lt
        if(
                        operation == BinaryOperator.gt
                ||
                        operation == BinaryOperator.lt
        ) {
            if(typeLValue instanceof IntType && typeRValue instanceof IntType) { // int * int -> bool
                return new BoolType();
            }
            if(typeLValue instanceof NoType && typeRValue instanceof NoType) { // noType * noType -> noType
                return new NoType();
            }
            if(typeLValue instanceof NoType && typeRValue instanceof IntType) { // noType * int -> bool
                return new BoolType();
            }
            if(typeLValue instanceof IntType && typeRValue instanceof NoType) { // int * noType -> bool
                return new BoolType();
            }

            //Error
            binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), operation.name()));
        }

        //and, or
        if(
                        operation == BinaryOperator.and
                        ||
                        operation == BinaryOperator.or
        ) {
            if(typeLValue instanceof BoolType && typeRValue instanceof BoolType) { // bool * bool -> bool
                return new BoolType();
            }
            if(typeLValue instanceof NoType && typeRValue instanceof NoType) { // noType * noType -> noType
                return new NoType();
            }
            if(typeLValue instanceof NoType && typeRValue instanceof BoolType) { // noType * bool -> bool
                return new BoolType();
            }
            if(typeLValue instanceof BoolType && typeRValue instanceof NoType) { // bool * noType -> bool
                return new BoolType();
            }

            //Error
            binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), operation.name()));
        }

        //assign
        if(
                operation == BinaryOperator.assign
        ) {
            if(typeLValue == typeRValue) {
                return typeLValue;
            }
            if(typeLValue instanceof NoType && typeRValue instanceof NoType) {
                return new NoType();
            }
            if(typeLValue instanceof NoType) {
                return typeRValue;
            }
            if(typeRValue instanceof NoType) {
                return typeLValue;
            }

            //Error
            binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), operation.name()));
        }
        return null;
    }

    @Override
    public Type visit(UnaryExpression unaryExpression) {
        //Todo
        return null;
    }

    @Override
    public Type visit(FunctionCall funcCall) {
        //Todo
        return null;
    }

    @Override
    public Type visit(Identifier identifier) {
        //Todo
        return null;
    }

    @Override
    public Type visit(ListAccessByIndex listAccessByIndex) {
        //Todo
        return null;
    }

    @Override
    public Type visit(StructAccess structAccess) {
        //Todo
        return null;
    }

    @Override
    public Type visit(ListSize listSize) {
        //Todo
        return null;
    }

    @Override
    public Type visit(ListAppend listAppend) {
        //Todo
        return null;
    }

    @Override
    public Type visit(ExprInPar exprInPar) {
        //Todo
        return null;
    }

    @Override
    public Type visit(IntValue intValue) {
        //Todo
        return null;
    }

    @Override
    public Type visit(BoolValue boolValue) {
        //Todo
        return null;
    }
}
