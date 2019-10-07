int main(){

 int inp = 10;

 if(inp > 0){
   while(inp > 0){
     inp = inp - 1;
   }
 }else{
   inp = 0;
 }

 assert(inp == 0);
 return 0;
}