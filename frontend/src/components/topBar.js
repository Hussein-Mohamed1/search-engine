import {twMerge} from "tailwind-merge";
import {SearchBar} from "@/components/searchBar";

/* Top search bar and logo in the /search route */
export function TopBar({...props}) {
    return (

        <div className={twMerge("flex flex-row", "space-x-2", props.className)}>

            {/* Lumos Logo */}
            <div className="flex flex-row text-xl font-extrabold font-mono text-center items-center">
                <div className={"tracking-widest"}>Lumos</div>
                <div className="text-md">💡</div>
            </div>

            {/* Search Bar */}
            <SearchBar variant={"search"} />
        </div>)
}