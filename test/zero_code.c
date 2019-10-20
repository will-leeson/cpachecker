extern void __VERIFIER_error() __attribute__ ((__noreturn__));
int main(){

 int inp = 10;

 if(inp > 0){
   while(inp > 0){
     inp = inp - 1;
   }
 }else{
   inp = 0;
 }

 if(inp != 0){
  __VERIFIER_error();
 }

 return 0;
}