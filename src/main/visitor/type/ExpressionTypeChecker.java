package main.visitor.type;

import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.operators.UnaryOperator;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.types.*;
import main.ast.types.primitives.BoolType;
import main.ast.types.primitives.IntType;
import main.ast.types.primitives.VoidType;
import main.compileError.typeError.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.StructSymbolTableItem;
import main.symbolTable.items.SymbolTableItem;
import main.symbolTable.items.VariableSymbolTableItem;
import main.visitor.Visitor;

import java.util.ArrayList;

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
        UnaryOperator operator;
        Expression rValue;

        rValue = unaryExpression.getOperand();
        operator = unaryExpression.getOperator();

        Type typeRValue = rValue.accept(this);

        // minus
        if(
                operator == UnaryOperator.minus
        ) {
            if(typeRValue instanceof IntType) { // int -> int
                return new IntType();
            }
            if(typeRValue instanceof NoType) { // noType -> noType
                return new NoType();
            }

            //Error
            unaryExpression.addError(new UnsupportedOperandType(unaryExpression.getLine(), operator.name()));
        }

        // not
        if(
                operator == UnaryOperator.not
        ) {
            if(typeRValue instanceof BoolType) { // bool -> bool
                return new BoolType();
            }
            if(typeRValue instanceof NoType) { // noType -> noType
                return new NoType();
            }

            //Error
            unaryExpression.addError(new UnsupportedOperandType(unaryExpression.getLine(), operator.name()));
        }
        return null;
    }

    @Override
    public Type visit(FunctionCall funcCall) {
        Type functionCallType = funcCall.getInstance().accept(this);
        if (functionCallType instanceof FptrType) {
            ArrayList<Expression> args = funcCall.getArgs();
            for (Expression arg : args) {
                if (!(arg.accept(this) instanceof FptrType)) {
                    funcCall.addError(new ArgsInFunctionCallNotMatchDefinition(funcCall.getLine()));
                    return new NoType();
                }
            }
            return ((FptrType) functionCallType).getReturnType();
        } else {
            funcCall.addError(new CallOnNoneFptrType(funcCall.getLine()));
            return new NoType();
        }
    }

    @Override
    public Type visit(Identifier identifier) {
        try {
            SymbolTableItem variableSymbolTableItem = SymbolTable.top.getItem(VariableSymbolTableItem.START_KEY + identifier.getName());
            return ((VariableSymbolTableItem) variableSymbolTableItem).getType();
        } catch (ItemNotFoundException error) {
            identifier.addError(new VarNotDeclared(identifier.getLine(), identifier.getName()));
            return new NoType();
        }
    }

    @Override
    public Type visit(ListAccessByIndex listAccessByIndex) {
        Expression list;
        Expression index;

        Type listType;
        Type indexType;

        list = listAccessByIndex.getInstance();
        index = listAccessByIndex.getIndex();
        listType = list.accept(this);
        indexType = index.accept(this);

        if(indexType instanceof IntType && listType instanceof ListType) {
            return new ListType(listType);
        }
        if(indexType instanceof IntType && listType instanceof NoType) {
            return new NoType();
        }
        if(!(indexType instanceof IntType)) {
            listAccessByIndex.addError(new ListIndexNotInt(listAccessByIndex.getLine()));
        } else {
            listAccessByIndex.addError(new AccessByIndexOnNonList(listAccessByIndex.getLine()));
        }
        return null;
    }

    @Override
    public Type visit(StructAccess structAccess) {
        Type structType = structAccess.getInstance().accept(this);
        Identifier structElement = structAccess.getElement();
        try {
            if (structType instanceof StructType) {
                var structKey = StructSymbolTableItem.START_KEY + ((StructType) structType).getStructName().getName();
                SymbolTableItem structSymbolTableItem = SymbolTable.root.getItem(structKey);
                try {
                    var variableKey = VariableSymbolTableItem.START_KEY + structElement.getName();
                    SymbolTableItem variableSymbolTableItem = ((StructSymbolTableItem) structSymbolTableItem).getStructSymbolTable().getItem(variableKey);
                    return  ((VariableSymbolTableItem)variableSymbolTableItem).getType();
                } catch (ItemNotFoundException e) {
                    structAccess.addError(new StructMemberNotFound(structAccess.getLine(),((StructSymbolTableItem) structSymbolTableItem).getStructDeclaration().getStructName().getName(), structElement.getName()));
                    return new NoType();
                }
            } else {
                structAccess.addError(new AccessOnNonStruct(structAccess.getLine()));
                return new NoType();
            }
        } catch (ItemNotFoundException e) {
            structAccess.addError(new StructNotDeclared(structAccess.getLine(), ((StructType) structType).getStructName().getName()));
            return new NoType();
        }
    }

    @Override
    public Type visit(ListSize listSize) {
        Type list = listSize.getArg().accept(this);
        if(list instanceof ListType) {
            return new IntType();
        } else {
            listSize.addError(new GetSizeOfNonList(listSize.getLine()));
            return new NoType();
        }
    }

    @Override
    public Type visit(ListAppend listAppend) {
        Type listType = listAppend.getListArg().accept(this);
        if(listType instanceof ListType) {
            Type listElementType = ((ListType) listType).getType();
            Type elementRValueType = listAppend.getElementArg().accept(this);
            if(listElementType.getClass() == elementRValueType.getClass()) {
                if(listElementType instanceof NoType) {
                    return new NoType();
                } else {
                    return new VoidType();
                }
            } else {
                listAppend.addError(new NewElementTypeNotMatchListType(listAppend.getLine()));
            }
        }
        if(listType instanceof NoType) {
            return new NoType();
        }
        else {
            listAppend.addError(new AppendToNonList(listAppend.getLine()));
        }
        return null;
    }

    @Override
    public Type visit(ExprInPar exprInPar) {
        ArrayList<Expression> inputExprs = exprInPar.getInputs();
        return inputExprs.get(0).accept(this);
    }

    @Override
    public Type visit(IntValue intValue) {
        return new IntType();
    }

    @Override
    public Type visit(BoolValue boolValue) {
        return new BoolType();
    }
}
