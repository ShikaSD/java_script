function myFunction(param1, param2) {
    var hello = "value";
    print(hello)
    print(param2)
    print(param1)
    anotherFunction()
}

function anotherFunction() {
    print("I am getting called!")
}

myFunction(.1, "")
print("Static init end.")
