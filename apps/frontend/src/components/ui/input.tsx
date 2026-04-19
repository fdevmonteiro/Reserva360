import * as React from "react"

import { cn } from "@/lib/utils"

const Input = React.forwardRef<HTMLInputElement, React.ComponentProps<"input">>(
  ({ className, type, ...props }, ref) => {
    return (
      <input
        type={type}
        className={cn(
 "flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-base",
    " [&::-webkit-calendar-picker-indicator]:opacity-70",
    " [&::-webkit-calendar-picker-indicator]:cursor-pointer",          className
        )}
        ref={ref}
        {...props}
      />
    )
  }
)
Input.displayName = "Input"

export { Input }
