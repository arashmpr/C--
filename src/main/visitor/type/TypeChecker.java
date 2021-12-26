package main.visitor.type;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.FunctionDeclaration;
import main.ast.nodes.declaration.MainDeclaration;
import main.ast.nodes.declaration.VariableDeclaration;
import main.ast.nodes.declaration.struct.StructDeclaration;
import main.ast.nodes.expression.BinaryExpression;
import main.ast.nodes.expression.Identifier;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.statement.*;
import main.ast.types.NoType;
import main.ast.types.StructType;
import main.ast.types.Type;
import main.ast.types.primitives.BoolType;
import main.ast.types.primitives.IntType;
import main.compileError.nameError.*;
import main.compileError.typeError.ConditionNotBool;
import main.compileError.typeError.StructNotDeclared;
import main.compileError.typeError.UnsupportedTypeForDisplay;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemAlreadyExistsException;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.FunctionSymbolTableItem;
import main.symbolTable.items.StructSymbolTableItem;
import main.symbolTable.items.SymbolTableItem;
import main.symbolTable.items.VariableSymbolTableItem;
import main.symbolTable.utils.graph.Graph;
import main.visitor.Visitor;

import java.util.ArrayList;

public class TypeChecker extends Visitor<Void> {
    ExpressionTypeChecker expressionTypeChecker = new ExpressionTypeChecker();

    private int newId = 1;
    private boolean firstVisit = true;
    private boolean isInStruct = false;
    private String curStructName;
    private final Graph<String> structHierarchy = new Graph<>();

    private void createStructSymbolTable(StructDeclaration structDec) {
        SymbolTable newSymbolTable = new SymbolTable();
        StructSymbolTableItem newSymbolTableItem = new StructSymbolTableItem(structDec);
        newSymbolTableItem.setStructSymbolTable(newSymbolTable);
        try {
            SymbolTable.root.put(newSymbolTableItem);

        } catch (ItemAlreadyExistsException e) {
            DuplicateStruct exception = new DuplicateStruct(structDec.getLine(), structDec.getStructName().getName());
            structDec.addError(exception);
            String newName = newId + "@";
            newId += 1;
            structDec.setStructName(new Identifier(newName));
            try {
                StructSymbolTableItem newStructSym = new StructSymbolTableItem(structDec);
                newStructSym.setStructSymbolTable(newSymbolTable);
                SymbolTable.root.put(newStructSym);
            } catch (ItemAlreadyExistsException e1) { //Unreachable
            }
        }
    }


    private void createFunctionSymbolTable(FunctionDeclaration funcDec) {
        FunctionSymbolTableItem newSymbolTableItem = new FunctionSymbolTableItem(funcDec);
        try {
            SymbolTable.root.put(newSymbolTableItem);

        } catch (ItemAlreadyExistsException e) {
            DuplicateFunction exception = new DuplicateFunction(funcDec.getLine(), funcDec.getFunctionName().getName());
            funcDec.addError(exception);
            String newName = newId + "@";
            newId += 1;
            funcDec.setFunctionName(new Identifier(newName));
            try {
                FunctionSymbolTableItem newFuncSym = new FunctionSymbolTableItem(funcDec);
                SymbolTable.root.put(newFuncSym);
            } catch (ItemAlreadyExistsException e1) { //Unreachable
            }
        }
    }

    private boolean hasConflict(String key) {
        try {
            SymbolTable.root.getItem(key);
            return true;
        } catch (ItemNotFoundException exception) {
            return false;
        }
    }

    private void checkCycle(ArrayList<StructDeclaration> structs){
        for(StructDeclaration struct : structs){
            String structName = struct.getStructName().getName();
            if(structHierarchy.isSecondNodeAncestorOf(structName, structName)){
                CyclicDependency exception = new CyclicDependency(struct.getLine(), structName);
                struct.addError(exception);
            }
        }
    }

    public void TypeChecker(){
        this.expressionTypeChecker = new ExpressionTypeChecker();
    }

    @Override
    public Void visit(Program program) {
        SymbolTable root = new SymbolTable();
        SymbolTable.root = root;
        SymbolTable.push(root);
        for (StructDeclaration structDec : program.getStructs()) {
            createStructSymbolTable(structDec);
            try {
                structHierarchy.addNode(structDec.getStructName().getName());
            }
            catch (Exception e){
            }
        }

        for (FunctionDeclaration funcDec : program.getFunctions()) {
            if (hasConflict(StructSymbolTableItem.START_KEY + funcDec.getFunctionName().getName())) {
                FunctionStructConflict exception = new FunctionStructConflict(funcDec.getLine(), funcDec.getFunctionName().getName());
                funcDec.addError(exception);
            }
            createFunctionSymbolTable(funcDec);
        }

        for (StructDeclaration structDec : program.getStructs()) {
            try {
                String key = StructSymbolTableItem.START_KEY + structDec.getStructName().getName();
                StructSymbolTableItem structSymbolTableItem = (StructSymbolTableItem) SymbolTable.root.getItem(key);
                SymbolTable.push(structSymbolTableItem.getStructSymbolTable());
                isInStruct = true;
                curStructName = structDec.getStructName().getName();
                structDec.accept(this);
                isInStruct = false;
                SymbolTable.pop();
            } catch (ItemNotFoundException e) {
            }
        }

        checkCycle(program.getStructs());

        for (FunctionDeclaration funcDec : program.getFunctions()) {
            SymbolTable.push(new SymbolTable());
            funcDec.accept(this);
            SymbolTable.pop();
        }

        SymbolTable.push(new SymbolTable());
        program.getMain().accept(this);
        SymbolTable.pop();

        return null;
    }

    @Override
    public Void visit(FunctionDeclaration functionDec) {
        try {
            var key = FunctionSymbolTableItem.START_KEY + functionDec.getFunctionName().getName();
            SymbolTableItem functionSymbolTable = SymbolTable.root.getItem(key);
            SymbolTable.push(((FunctionSymbolTableItem) functionSymbolTable).getFunctionSymbolTable());
            functionDec.getBody().accept(this);
            for (VariableDeclaration arg : functionDec.getArgs()) {
                arg.accept(expressionTypeChecker);
            }
            functionDec.getBody();
            Type functionReturnType = functionDec.getReturnType();
            if(functionReturnType instanceof StructType) {
                try {
                    SymbolTable.top.getItem(StructSymbolTableItem.START_KEY + ((StructType) functionReturnType).getStructName());
                } catch (ItemNotFoundException e) {
                    functionDec.addError(new StructNotDeclared(functionDec.getLine(), ((StructType) functionReturnType).getStructName().getName()));
                    functionDec.setReturnType(new NoType());
                }
            }
            SymbolTable.pop();
        } catch (ItemNotFoundException e) {

        }
        return null;
    }

    @Override
    public Void visit(MainDeclaration mainDec) {
        SymbolTable mainSymbolTable = new SymbolTable(SymbolTable.root);
        SymbolTable.push(mainSymbolTable);
        mainDec.getBody().accept(this);
        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(VariableDeclaration variableDec) {
        String name = variableDec.getVarName().getName();

        if(isInStruct && variableDec.getVarType() instanceof StructType){
            StructType structType = (StructType) variableDec.getVarType();
            try {
                structHierarchy.addNodeAsParentOf(structType.getStructName().getName(), curStructName);
            }
            catch (Exception e){
            }
        }

        if (hasConflict(StructSymbolTableItem.START_KEY + name)) {
            VarStructConflict exception = new VarStructConflict(variableDec.getLine(), name);
            variableDec.addError(exception);
        }

        if (hasConflict(FunctionSymbolTableItem.START_KEY + name)) {
            VarFunctionConflict exception = new VarFunctionConflict(variableDec.getLine(), name);
            variableDec.addError(exception);
        }

        VariableSymbolTableItem variableSymbolTableItem = new VariableSymbolTableItem(variableDec.getVarName());
        try {
            SymbolTable.top.getItem(variableSymbolTableItem.getKey());
            DuplicateVar exception = new DuplicateVar(variableDec.getLine(), name);
            variableDec.addError(exception);
        } catch (ItemNotFoundException exception2) {
            try {
                SymbolTable.top.put(variableSymbolTableItem);
            } catch (ItemAlreadyExistsException exception3) {
            }
        }
        return null;
    }

    @Override
    public Void visit(StructDeclaration structDec) {
        try {
            var key = StructSymbolTableItem.START_KEY + structDec.getStructName().getName();
            SymbolTable structSymbolTableItem = ((StructSymbolTableItem)(SymbolTable.root.getItem(key))).getStructSymbolTable();
            SymbolTable.push(structSymbolTableItem);
            structDec.getBody().accept(this);
            firstVisit = false;
            structDec.getBody().accept(this);
            firstVisit = true;
            SymbolTable.pop();
        } catch (ItemNotFoundException e) {
            structDec.addError(new StructNotDeclared(structDec.getLine(), structDec.getStructName().getName()));
        }
        return null;
    }

    @Override
    public Void visit(SetGetVarDeclaration setGetVarDec) {
        String name = setGetVarDec.getVarName().getName();
        if (firstVisit) {
            setGetVarDec.getVarDec().accept(this);
            SymbolTable newSym = new SymbolTable();
            FunctionDeclaration funcDec = new FunctionDeclaration();

            funcDec.setFunctionName(new Identifier(name));
            funcDec.setReturnType(setGetVarDec.getVarType());
            funcDec.setArgs(setGetVarDec.getArgs());
            FunctionSymbolTableItem newItem = new FunctionSymbolTableItem(funcDec);
            newItem.setFunctionSymbolTable(newSym);
            try {
                SymbolTable.top.put(newItem);
            } catch (ItemAlreadyExistsException e) {
                setGetVarDec.setVarName(new Identifier(name + "@" + newId));
                funcDec.setFunctionName(new Identifier(name + "@" + newId));
                newId += 1;
                FunctionSymbolTableItem fSym = new FunctionSymbolTableItem(funcDec);
                fSym.setFunctionSymbolTable(newSym);
                try{
                    SymbolTable.top.put(fSym);
                }catch (ItemAlreadyExistsException e2) {
                }
            }
        }
        else {
            try {
                String key = FunctionSymbolTableItem.START_KEY + name;
                FunctionSymbolTableItem fItem = (FunctionSymbolTableItem) SymbolTable.top.getItem(key);
                SymbolTable sym = fItem.getFunctionSymbolTable();
                sym.pre = SymbolTable.top;
                SymbolTable.push(sym);
                for (VariableDeclaration arg : setGetVarDec.getArgs())
                    arg.accept(expressionTypeChecker);
                SymbolTable.pop();
            } catch (ItemNotFoundException e) {
            }
        }
        return null;
    }

    @Override
    public Void visit(AssignmentStmt assignmentStmt) {
        BinaryExpression binaryExpression = new BinaryExpression(assignmentStmt.getLValue(), assignmentStmt.getRValue(), BinaryOperator.assign);
        binaryExpression.accept(expressionTypeChecker);
        return null;
    }

    @Override
    public Void visit(BlockStmt blockStmt) {
        ArrayList<Statement> blockStmts = blockStmt.getStatements();
        for (Statement stmt : blockStmts) {
            if (firstVisit)
                stmt.accept(this);
            if (!firstVisit && stmt instanceof SetGetVarDeclaration)
                stmt.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConditionalStmt conditionalStmt) {
        SymbolTable.push(new SymbolTable(SymbolTable.top));
        Type conditionalType = conditionalStmt.getCondition().accept(expressionTypeChecker);
        if (!(conditionalType instanceof BoolType) && !(conditionalType instanceof NoType)) {
            conditionalStmt.addError(new ConditionNotBool(conditionalStmt.getLine()));
        }
        conditionalStmt.getThenBody().accept(this);
        SymbolTable.pop();
        if (conditionalStmt.getElseBody() != null) {
            SymbolTable.push(new SymbolTable(SymbolTable.top));
            conditionalStmt.getElseBody().accept(this);
            SymbolTable.pop();
        }
        return null;
    }

    @Override
    public Void visit(FunctionCallStmt functionCallStmt) {
        //Todo
        return null;
    }

    @Override
    public Void visit(DisplayStmt displayStmt) {
        Type argType = displayStmt.getArg().accept(expressionTypeChecker);
        if(!(argType instanceof IntType) && !(argType instanceof BoolType) && !(argType instanceof NoType)) {
            displayStmt.addError(new UnsupportedTypeForDisplay(displayStmt.getLine()));
        }
        return null;
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        //todo
        return null;
    }

    @Override
    public Void visit(LoopStmt loopStmt) {
        Type conditionType = loopStmt.getCondition().accept(expressionTypeChecker);
        if (conditionType instanceof BoolType) {
            return null;
        } else if(conditionType instanceof NoType) {
            return null;
        } else {
            loopStmt.addError(new ConditionNotBool(loopStmt.getLine()));
        }

        loopStmt.getBody().accept(this);
        return null;
    }

    @Override
    public Void visit(VarDecStmt varDecStmt) {
        ArrayList<VariableDeclaration> varDecs = varDecStmt.getVars();
        for (VariableDeclaration varDec : varDecs) {
            varDec.accept(expressionTypeChecker);
        }
        return null;
    }

    @Override
    public Void visit(ListAppendStmt listAppendStmt) {
        listAppendStmt.getListAppendExpr().accept(expressionTypeChecker);
        return null;
    }

    @Override
    public Void visit(ListSizeStmt listSizeStmt) {
        listSizeStmt.getListSizeExpr().accept(expressionTypeChecker);
        return null;
    }
}
