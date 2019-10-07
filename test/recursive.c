
int main(){

  fib(5);

  return 0;
}


int fib(int n);

int fib(int n){

  if(n <= 1){
    return 1;
  }

  int x = sub_fib(n);
  return x;
}

int sub_fib(int n){
  return fib(n-1) + fib(n-2);
}