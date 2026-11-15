export function createSessionGuard(): {
 current(): number; isCurrent(value:number): boolean; invalidate(): number;
 refresh<T>(run:()=>Promise<T>): Promise<T>;
};
export function staleSessionError(): Error & {code:string};
