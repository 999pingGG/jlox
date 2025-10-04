import sys

def define_visitor(class_names, base_name):
    return f'''    interface Visitor<R> {{
{'\n'.join([f'        R visit{type_name}{base_name}({type_name} {base_name.lower()});' for type_name in class_names])}
    }}'''

def define_ast(output_directory, base_name, types):
    path = f'{output_directory}/{base_name}.java'
    with open(path, 'w', encoding='utf-8') as out:
        class_names = [type[0] for type in types]
        fields = [[[split for split in field.strip().split(' ')] for field in type[1].split(',')] for type in types]
        out.write(
f'''package me.elinge.lox;

import java.util.List;

abstract class {base_name} {{
{define_visitor(class_names, base_name)}

{'\n\n'.join([f'''    static class {class_names[i]} extends {base_name} {{
        {class_names[i]}({types[i][1]}) {{
{'\n'.join([f'            this.{field[1]} = {field[1]};' for field in fields[i]])}
        }}

        @Override
        <R> R accept(Visitor<R> visitor) {{
            return visitor.visit{class_names[i]}{base_name}(this);
        }}

{'\n'.join([f'        final {field[0]} {field[1]};' for field in fields[i]])}
    }}''' for i in range(len(types))])}

    abstract <R> R accept(Visitor<R> visitor);
}}
''')

if len(sys.argv) != 2:
    print('Usage: generate_ast <path to src/me/elinge/lox/>')
    sys.exit(64) # EX_USAGE

output_directory = sys.argv[1]
define_ast(
    output_directory,
    'Expr',
    [
        ('Assign',   'Token name, Expr value'),
        ('Binary',   'Expr left, Token operator, Expr right'),
        ('Grouping', 'Expr expression'),
        ('Literal',  'Object value'),
        ('Logical',  'Expr left, Token operator, Expr right'),
        ('Unary',    'Token operator, Expr right'),
        ('Ternary',  'Expr left, Token operator1, Expr middle, Token operator2, Expr right'),
        ('Variable', 'Token name'),
    ])
define_ast(
	output_directory,
	'Stmt',
	[
		('Block',      'List<Stmt> statements'),
		('Expression', 'Expr expression'),
		('If',         'Expr condition, Stmt thenBranch, Stmt elseBranch'),
		('Print',      'Expr expression'),
		('Var',        'Token name, Expr initializer'),
		('While',      'Expr condition, Stmt body'),
	])
