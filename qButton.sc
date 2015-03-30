+ QButton {
     setBackgroundColor { |i, color|
         var st = this.states, currentVal = this.value;
         st[i][2] = color;
         this.states = st;
         this.value = currentVal;
         this.refresh;
         ^color
     }
} 